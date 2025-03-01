package com.github.alinski.service

import com.github.alinski.io.{ApiClient, CollectionEndpoint, JsonEndpoint, SingleResourceEndpoint}
import com.github.alinski.model.Book
import sttp.client4.Response
import sttp.model.QueryParams
import sttp.model.Uri.PathSegment
import upickle.default.*

import java.time.LocalDate
import scala.util.{Failure, Success, Try}

object BookApiService:
  lazy private val getBookByIdEndpoint: SingleResourceEndpoint = new JsonEndpoint with SingleResourceEndpoint:
    override def apiClient: ApiClient[Book] = GoogleBooksApiClient
    override def fetchById(id: String): Either[Response[String], Response[ujson.Value]] =
      getResponse(
        endpoint = apiClient.baseUri,
        pathSegments = List("volumes", id).map(PathSegment(_))
      )

  lazy private val getBookByIsbnEndpoint: SingleResourceEndpoint = new JsonEndpoint with SingleResourceEndpoint:
    override def apiClient: ApiClient[Book] = GoogleBooksApiClient
    override def fetchById(isbn: String): Either[Response[String], Response[ujson.Value]] =
      getResponse(
        endpoint = apiClient.baseUri,
        pathSegments = List("volumes").map(PathSegment(_)),
        queryParams = QueryParams.fromMap(Map("q" -> s"isbn:$isbn"))
      )

  lazy private val searchBooksEndpoint: CollectionEndpoint = new JsonEndpoint with CollectionEndpoint:
    override def apiClient: ApiClient[Book] = GoogleBooksApiClient
    override def search(
        query: String,
        limit: Option[Int] = None
    ): Either[Response[String], Response[ujson.Value]] =
      val queryParams = limit match
        case Some(n) => QueryParams.fromMap(Map("q" -> query, "maxResults" -> n.toString))
        case None    => QueryParams.fromMap(Map("q" -> query))

      getResponse(
        endpoint = apiClient.baseUri,
        pathSegments = List("volumes").map(PathSegment(_)),
        queryParams = queryParams
      )

trait ApiService[T]:
  def getById(id: String): Either[Exception, T]
  def search(query: String, limit: Int): Either[Exception, LazyList[T]]

case class BookApiService(
    getByIdEndpoint: SingleResourceEndpoint = getBookByIdEndpoint,
    getByIsbnEndpoint: SingleResourceEndpoint = getBookByIsbnEndpoint,
    searchEndpoint: CollectionEndpoint = searchBooksEndpoint
) extends ApiService[Book]:
  import com.github.alinski.serde.ParsingUtils.jsonExtensions.*

  def getById(id: String): Either[Exception, Book] =
    getByIdEndpoint.fetchById(id) match
      case Left(err) =>
        Left(ujson.ParseException(s"Invalid ujson.Value response, error: $err", 0))
      case Right(response: Response[ujson.Value]) =>
        parseBookResult(response.body)

  def getByIsbn(isbn: String): Either[Exception, Book] =
    getByIsbnEndpoint.fetchById(isbn) match
      case Left(err) =>
        Left(ujson.ParseException(s"Invalid ujson.Value response, error: $err", 0))
      case Right(response: Response[ujson.Value]) =>
        parseBookResults(response.body)

  def search(query: String, limit: Int = 10): Either[Exception, LazyList[Book]] =
    searchEndpoint.search(query, Some(limit)) match
      case Left(err) =>
        Left(ujson.ParseException("Invalid ujson.Value response", 0))
      case Right(response: Response[ujson.Value]) =>
        response.body.obj.get("items") match
          case None =>
            Right(LazyList.empty) // No results found, return empty list
          case Some(items) =>
            items.arr
              .map(item => parseBookResult(item))
              .foldLeft(Right(LazyList.empty[Book]): Either[Exception, LazyList[Book]]) {
                case (Left(err), _)              => Left(err)
                case (_, Left(err))              => Left(err)
                case (Right(books), Right(book)) => Right(books :+ book)
              }

  private def parseBookResult(bookJson: ujson.Value): Either[Exception, Book] =
    try
      val volumeInfo = bookJson.obj("volumeInfo").obj

      // Extract ISBNs
      val industryIdentifiers = volumeInfo.get("industryIdentifiers").map(_.arr).getOrElse(Seq.empty)
      val isbn10 = industryIdentifiers.find(id => id.obj("type").str == "ISBN_10").map(_.obj("identifier").str)
      val isbn13 = industryIdentifiers.find(id => id.obj("type").str == "ISBN_13").map(_.obj("identifier").str)

      // Extract image links
      val imageLinksMap =
        volumeInfo.get("imageLinks").map(links => links.obj.map { case (key, value) => key -> value.str }.toMap)

      // Parse date
      val dateStr = volumeInfo.get("publishedDate").map(_.str)
      val publishedDate = dateStr.flatMap(str =>
        try
          // Handle different date formats (YYYY, YYYY-MM, YYYY-MM-DD)
          if str.matches("\\d{4}") then Some(LocalDate.of(str.toInt, 1, 1))
          else if str.matches("\\d{4}-\\d{2}") then
            val parts = str.split("-")
            Some(LocalDate.of(parts(0).toInt, parts(1).toInt, 1))
          else Some(LocalDate.parse(str))
        catch case _: Exception => None
      )

      val book = Book(
        googleId = bookJson.obj("id").str,
        title = volumeInfo("title").str,
        subtitle = volumeInfo.get("subtitle").map(_.str),
        authors = volumeInfo.get("authors").map(_.arr.map(_.str).toList).getOrElse(List.empty),
        description = volumeInfo.get("description").map(_.str),
        pageCount = volumeInfo.get("pageCount").map(_.num.toInt),
        publishedDate = publishedDate,
        publisher = volumeInfo.get("publisher").map(_.str),
        isbn10 = isbn10,
        isbn13 = isbn13,
        language = volumeInfo.get("language").map(_.str),
        categories = volumeInfo.get("categories").map(_.arr.map(_.str).toList),
        imageLinks = imageLinksMap,
        selfLink = bookJson.obj.get("selfLink").map(_.str),
        previewLink = volumeInfo.get("previewLink").map(_.str),
        infoLink = volumeInfo.get("infoLink").map(_.str)
      )

      Right(book)
    catch case e: Exception => Left(e)

  private def parseBookResults(content: ujson.Value): Either[Exception, Book] =
    content.obj.get("items") match
      case None                             => Left(ujson.ParseException("No items found in response", 0))
      case Some(items) if items.arr.isEmpty => Left(ujson.ParseException("Empty items array", 0))
      case Some(items)                      => parseBookResult(items.arr.head)
