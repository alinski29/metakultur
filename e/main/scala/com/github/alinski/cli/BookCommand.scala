package com.github.alinski.cli

import caseapp.*
import com.github.alinski.io.{FileWriter, WriteOptions}
import com.github.alinski.model.Book
import com.github.alinski.serde.Serializer.MarkdownTable
import com.github.alinski.serde.{FileFormat, Serializer}
import com.github.alinski.service.BookApiService

import scala.annotation.tailrec

object BookCommand extends Command[BookOptions]:
  lazy val bookService = BookApiService()
  override def name    = "book"

  def run(options: BookOptions, args: RemainingArgs): Unit =
    val maybeBooks = (options.isbn, options.goodreadsCSVFile) match
      case (Some(_), Some(_)) =>
        Left(IllegalArgumentException("Cannot provide both ISBN and Goodreads CSV file"))
      case (Some(isbn), _) =>
        bookService.getByIsbn(isbn).map(LazyList(_))
      case (_, Some(filePath)) =>
        Left(IllegalArgumentException("Goodreads CSV import not implemented yet"))
      case _ =>
        buildSearchQuery(options, args) match
          case Some(query) => userPromptInteraction(bookService, query, options.limit).map(LazyList(_))
          case None =>
            Left(IllegalArgumentException("No id, file or search query provided. At least one is required"))

    val format          = resolveOutputFormat(options.outputFormat, options.outputFile)
    val booksSerialized = maybeBooks.map(_.map(Serializer.serialize(_, format)))

    options.outputFile.map(os.Path(_)) match
      case None =>
        booksSerialized match
          case Right(books) => books.foreach(println)
          case Left(err) if err.getMessage.contains("No items found") || err.getMessage.contains("Empty items") =>
            println(s"No books found for isbn: ${options.isbn.getOrElse("")}")
      case Some(path) =>
        val result = for
          books <- booksSerialized
          file  <- FileWriter.write(books, path, WriteOptions.default, Some(format))
        yield file
        result match
          case Right(file) => scribe.info(s"Books written to $file")
          case Left(e)     => throw e

  private def buildSearchQuery(options: BookOptions, args: RemainingArgs): Option[String] =
    (options.title, options.author, args.all.nonEmpty) match
      case (Some(title), Some(author), _) =>
        Some(s"intitle:$title+inauthor:$author")
      case (Some(title), None, _) =>
        Some(s"intitle:$title")
      case (None, Some(author), _) =>
        Some(s"inauthor:$author")
      case (_, _, true) =>
        Some(args.all.mkString(" "))
      case _ =>
        None

  @tailrec
  private def userPromptInteraction(
      service: BookApiService,
      query: String,
      limit: Int
  ): Either[Exception, Book] =
    service.search(query, limit) match
      case Left(err) => Left(err)
      case Right(books) if books.nonEmpty =>
        val selection = promptUserToSelectBook(books)
        selection
      case Right(books) =>
        println(s"No results found for your search query: '$query'. Enter a new query")
        val newQuery = scala.io.StdIn.readLine()
        userPromptInteraction(service, newQuery, limit)

  private def promptUserToSelectBook(books: LazyList[Book]): Either[Exception, Book] =
    if books.isEmpty then return Left(Exception("No books found"))
    println("Select a book by its index: \n")

    val maxLength = 60
    val headers   = Seq("Index", "Title", "Author(s)", "Year", "Pages", "Publisher", "ISBN")
    val rows = books.zipWithIndex.map { case (book, index) =>
      val authors   = book.authors.mkString(", ")
      val year      = book.publishedDate.map(_.getYear.toString).getOrElse("N/A")
      val pages     = book.pageCount.map(_.toString).getOrElse("N/A")
      val isbn      = book.isbn13.orElse(book.isbn10).getOrElse("N/A")
      val publisher = book.publisher.getOrElse("N/A")

      Seq(
        (index + 1).toString,
        book.title.take(maxLength),
        authors.take(maxLength),
        year,
        pages,
        publisher,
        isbn
      )
    }

    val markdownTable = MarkdownTable.serialize(rows, headers)
    println(markdownTable)

    val selectedIndex = scala.io.StdIn.readInt() - 1
    if selectedIndex < 0 || selectedIndex >= books.size then Left(Exception("Invalid selection"))
    else Right(books(selectedIndex))

  private def resolveOutputFormat(outputFormat: Option[String], outputFile: Option[String]): FileFormat =
    outputFormat
      .map(FileFormat(_))
      .orElse(outputFile.map(FileFormat(_)))
      .getOrElse(FileFormat.Json)
