package com.github.alinski.cli

import caseapp.*
import com.github.alinski.cli.Helpers.*
import com.github.alinski.io.{FileWriter, WriteOptions}
import com.github.alinski.model.{Book, ReadState}
import com.github.alinski.serde.Serializer.MarkdownTable
import com.github.alinski.serde.Serializer
import com.github.alinski.service.{BookApiService, GoodreadsCsvReaderService}

import scala.annotation.tailrec
import java.time.LocalDate

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
        GoodreadsCsvReaderService
          .read(os.Path(filePath), options.shared.from.flatMap(parseDate), options.shared.to.flatMap(parseDate))
          .map(books => if options.shared.enrich then enrichBooks(bookService, books) else books)
      case _ =>
        buildSearchQuery(options, args) match
          case Some(query) =>
            userPromptInteraction(bookService, query, options.shared.limit).map(LazyList(_))
          case None =>
            Left(IllegalArgumentException("No id, file or search query provided. At least one is required"))

    // Apply user preferences if rate option is enabled and we have a direct ISBN lookup
    val maybeBooksWithPreferences =
      if options.shared.rate && maybeBooks.map(_.size).getOrElse(0) == 1
      then maybeBooks.map(_.map(collectUserPreferences))
      else maybeBooks

    maybeBooksWithPreferences match
      case Left(err) if err.getMessage.contains("No items found") || err.getMessage.contains("Empty items") =>
        println(s"No book found for isbn: ${options.isbn.getOrElse("")}")
        sys.exit(0)
      case Left(err) =>
        throw err
      case Right(books) if books.isEmpty && options.isbn.nonEmpty =>
        println(s"No book found for isbn: ${options.isbn.getOrElse("")}")
        sys.exit(0)
      case Right(books) => ()

    val writeOpts = WriteOptions.default
    val maybePath = options.shared.output.map(os.Path(_))
    maybePath.foreach { path =>
      if !path.toIO.exists() && writeOpts.makeDirs
      then os.makeDir.all(path)
      else if !path.toIO.exists() && !writeOpts.makeDirs then
        throw IllegalArgumentException(s"Path $path does not exist and makeDirs is set to false")
    }

    val books  = maybeBooksWithPreferences.getOrElse(LazyList.empty[Book])
    val format = resolveOutputFormat(options.shared.outputFormat, options.shared.output)

    maybePath match
      case None =>
        books.foreach(book => println(Serializer.serialize(book, format)))
      case Some(path) if path.ext.isBlank =>
        books.foreach { book =>
          val filePath = path / createValidFileName(book.title, format.toString)
          val output   = Serializer.serialize(book, format)
          FileWriter.write(LazyList(output), filePath, writeOpts) match
            case Right(file) =>
              scribe.info(s"Written to $file")
            case Left(err) =>
              scribe.error(s"Failed to write book to ${filePath}, error: ${err.getMessage}")
              throw err
        }
      case Some(path) =>
        val output = books.map(Serializer.serialize(_, format))
        FileWriter.write(output, path, writeOpts) match
          case Right(file) =>
            scribe.info(s"Written to $file")
          case Left(err) =>
            scribe.error(s"Failed to write books to file $path, error: ${err.getMessage}")
            throw err

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
      limit: Int,
  ): Either[Exception, Book] =
    service.search(query, limit) match
      case Left(err)                      => Left(err)
      case Right(books) if books.nonEmpty => promptUserToSelectBook(books)
      case Right(books) =>
        println(s"No results found for your search query: '$query'. Enter a new query or press Ctrl+C to exit")
        val newQuery = scala.io.StdIn.readLine()
        userPromptInteraction(service, newQuery, limit)

  private def promptUserToSelectBook(books: LazyList[Book]): Either[Exception, Book] =
    if books.isEmpty then return Left(Exception("No books found"))
    println("Select a book by its index: \n")

    val maxLength = 60
    val headers   = Seq("Index", "Title", "Author(s)", "Year", "Pages", "Publisher", "ISBN")
    val rows = books.zipWithIndex.map { case (book, index) =>
      Seq(
        (index + 1).toString,
        book.title.take(maxLength),
        book.author.take(maxLength),
        book.yearPublished.getOrElse("N/A"),
        book.pageCount.map(_.toString).getOrElse("N/A"),
        book.publisher.getOrElse("N/A"),
        book.isbn13.orElse(book.isbn10).getOrElse("N/A")
      )
    }

    val markdownTable = MarkdownTable.serialize(rows, headers)
    println(markdownTable)

    val selectedIndex = scala.io.StdIn.readInt() - 1
    if selectedIndex < 0 || selectedIndex >= books.size then Left(Exception("Invalid selection"))
    else Right(books(selectedIndex))

  private def collectUserPreferences(book: Book): Book =
    println(s"What's the reading status of ${book.title} by ${book.author}?")
    println("1. I've read it already")
    println("2. Currently reading")
    println("3. I want to read it (wishlist)")

    val statusChoice = scala.io.StdIn.readInt()
    val readStatus = statusChoice match
      case 1 => ReadState.Read
      case 2 => ReadState.CurrentlyReading
      case 3 => ReadState.Wishlist
      case _ =>
        println("Invalid choice, defaulting to wishlist")
        ReadState.Wishlist

    val (dateRead, personalRating) = if readStatus == ReadState.Read then
      println("When have you finished reading this book? Enter a date in format yyyy-MM-dd or leave blank.")
      val dateStr = scala.io.StdIn.readLine().trim
      val date =
        if dateStr.isEmpty then None
        else
          try Some(LocalDate.parse(dateStr))
          catch
            case _: Exception =>
              println("Invalid date format, using today's date")
              Some(LocalDate.now())

      println("Rate this book on a scale from 1 to 5")
      val rating =
        try
          val r = scala.io.StdIn.readInt()
          if r < 1 || r > 5 then
            println(s"Invalid rating: $r not between 1 and 5, no rating set")
            None
          else Some(r)
        catch
          case err: Exception =>
            println(s"Some error occured, can't set rating: ${err.getMessage}")
            None

      (date, rating)
    else (None, None)

    book.copy(
      readStatus = Some(readStatus),
      dateRead = dateRead,
      dateRated = if personalRating.isDefined then Some(LocalDate.now()) else None,
      personalRating = personalRating
    )

  private def enrichBooks(service: BookApiService, books: LazyList[Book]): LazyList[Book] =
    books.map { book =>
      book.isbn13.orElse(book.isbn10) match
        case None => book
        case Some(isbn) =>
          service.getByIsbn(isbn) match
            case Right(bookEnriched) =>
              book.copy(
                googleId = bookEnriched.googleId,
                description = bookEnriched.description,
                publishedDate = bookEnriched.publishedDate,
                language = bookEnriched.language,
                categories = bookEnriched.categories,
                thumbnail = bookEnriched.thumbnail,
                selfLink = bookEnriched.selfLink,
                previewLink = bookEnriched.previewLink,
                infoLink = bookEnriched.infoLink,
                pageCount = book.pageCount.orElse(bookEnriched.pageCount),
                publisher = book.publisher.orElse(bookEnriched.publisher),
                yearPublished = book.yearPublished.orElse(bookEnriched.yearPublished)
              )
            case Left(err) =>
              scribe.warn(s"Failed to fetch extra information for book: $isbn, title: ${book.title}")
              book
    }
