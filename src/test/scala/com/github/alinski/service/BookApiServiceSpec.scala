package com.github.alinski.service

import com.github.alinski.io.{ApiClient, CollectionEndpoint, JsonEndpoint, SingleResourceEndpoint}
import com.github.alinski.service.GoogleBooksApiClient.baseUri
import com.github.alinski.model.Book
import munit.FunSuite
import sttp.client4.Response
import sttp.model.Uri.UriContext
import sttp.model.*

class BookApiServiceSpec extends FunSuite:
  import BookApiServiceSpec.*

  test("Book retrieval by ISBN, 200 response") {
    bookService.getByIsbn(bookIsbn) match
      case Right(book) =>
        println(book)
        assert(book.title == "The Stranger")
        assert(book.author == "Albert Camus")
        assert(book.language.contains("en"))
        assert(book.publisher.contains("Knopf"))
        assert(book.pageCount.contains(148))
        assert(book.googleId.contains("35VcAAAAMAAJ"))
      // assert(book.imageLinks.nonEmpty)

      case Left(err) =>
        fail(s"Expected retrieval to succeed, but got error: $err")
  }

  test("Book retrieval by ID, 200 response") {
    bookService.getById(bookId) match
      case Right(book) =>
        assert(book.title == "The Stranger")
        assert(book.author == "Albert Camus")
        assert(book.language.contains("en"))
        assert(book.publisher.contains("Knopf"))
        assert(book.pageCount.contains(148))
        assert(book.googleId.contains("35VcAAAAMAAJ"))
      // assert(book.imageLinks.nonEmpty)

      case Left(err) =>
        fail(s"Expected retrieval to succeed, but got error: $err")
  }

  test("Book search, 200 non-empty response") {
    bookService.search("The Stranger") match
      case Right(books) =>
        assert(books.nonEmpty)
        assert(books.head.title == "The Stranger")
        assert(books.head.googleId.contains("35VcAAAAMAAJ"))
        assert(books.head.author == "Albert Camus")
        assert(books.head.publisher.contains("Knopf"))
      case Left(err) =>
        fail(s"Expected book search to succeed, but got error: $err")
  }

  // test("Book search with limit, 200 non-empty response") {
  //   bookService.search("The Stranger", 2) match
  //     case Right(books) =>
  //       assert(books.size <= 2)
  //       assert(books.head.title == "The Stranger")
  //       assert(books.head.googleId == "35VcAAAAMAAJ")
  //     case Left(err) =>
  //       fail(s"Expected book search to succeed, but got error: $err")
  // }

  test("Book search, 200 empty response") {
    bookService.search("empty") match
      case Right(books) =>
        assert(books.isEmpty)
      case Left(err) =>
        fail(s"Expected book search to succeed, but got error: $err")
  }

object BookApiServiceSpec:
  val apiResponsesPath = os.pwd / "src" / "test" / "resources" / "api_responses" / "google_books"
  val bookIsbn         = "9786064310583" // Using this ISBN but the response will be from the new JSON file
  val bookId           = "35VcAAAAMAAJ"  // Updated to match the new JSON file

  val getByIsbnEndpoint: SingleResourceEndpoint = new SingleResourceEndpoint with JsonEndpoint:
    override def apiClient: ApiClient[Book] = new ApiClient[Book]:
      val baseUri = uri"example.com"

    override def fetchById(isbn: String): Either[Response[String], Response[ujson.Value]] =
      def getRequestMeta(isbn: String) =
        val params    = QueryParams.fromMap(Map("q" -> s"isbn:$isbn"))
        val targetUri = baseUri.copy().addPath("volumes").addParams(params)
        RequestMetadata(Method("GET"), targetUri, Nil)

      isbn match
        case `bookIsbn` =>
          val json = ujson.read(os.read(apiResponsesPath / "list_volumes_200.json"))
          Right(Response.ok(json, getRequestMeta(bookIsbn)))
        case "empty" =>
          val json = ujson.read("""{"kind":"books#volumes","totalItems":0}""")
          Right(Response.ok(json, getRequestMeta("empty")))
        case _ =>
          Left(Response("Not found", StatusCode(404), RequestMetadata(Method("GET"), baseUri, Nil)))

  val getByIdEndpoint: SingleResourceEndpoint = new SingleResourceEndpoint with JsonEndpoint:
    override def apiClient: ApiClient[Book] = new ApiClient[Book]:
      val baseUri = uri"example.com"

    override def fetchById(id: String): Either[Response[String], Response[ujson.Value]] =
      def getRequestMeta(id: String) =
        val targetUri = baseUri.copy().addPath("volumes").addPath(id)
        RequestMetadata(Method("GET"), targetUri, Nil)

      id match
        case `bookId` =>
          // For simplicity, we'll use the same response as the list_volumes_200.json
          // but extract just the first item
          val json      = ujson.read(os.read(apiResponsesPath / "list_volumes_200.json"))
          val firstItem = json("items")(0)
          Right(Response.ok(firstItem, getRequestMeta(bookId)))
        case _ =>
          Left(Response("Not found", StatusCode(404), RequestMetadata(Method("GET"), baseUri, Nil)))

  val searchEndpoint: CollectionEndpoint = new CollectionEndpoint with JsonEndpoint:
    override def apiClient: ApiClient[Book] = new ApiClient[Book]:
      val baseUri = uri"example.com"

    override def search(
        query: String,
        limit: Option[Int] = None
    ): Either[Response[String], Response[ujson.Value]] =
      def getRequestMeta(query: String, limit: Option[Int]) =
        val params = limit match
          case Some(n) => QueryParams.fromMap(Map("q" -> query, "maxResults" -> n.toString))
          case None    => QueryParams.fromMap(Map("q" -> query))
        val targetUri = baseUri.copy().addPath("volumes").addParams(params)
        RequestMetadata(Method("GET"), targetUri, Nil)

      query match
        case q if q == "The Stranger" =>
          val json = ujson.read(os.read(apiResponsesPath / "list_volumes_200.json"))
          Right(Response.ok(json, getRequestMeta(query, limit)))
        case q if q == "empty" =>
          val json = ujson.read("""{"kind":"books#volumes","totalItems":0}""")
          Right(Response.ok(json, getRequestMeta(query, limit)))
        case q =>
          Left(Response("Not found", StatusCode(404), getRequestMeta(q, limit)))

  val bookService = BookApiService(
    getByIdEndpoint = getByIdEndpoint,
    getByIsbnEndpoint = getByIsbnEndpoint,
    searchEndpoint = searchEndpoint
  )
