package com.github.alinski.service

import com.github.alinski.io.CsvReader
import com.github.alinski.model.{Book, ReadState}

import scala.util.Try
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object GoodreadsCsvReaderService extends CsvReader[Book]:

  def read(
      path: os.Path,
      from: Option[LocalDate] = None,
      to: Option[LocalDate] = None
  ): Either[Throwable, LazyList[Book]] =
    def getDate(x: Book): LocalDate =
      x.dateRated.orElse(x.dateAdded).orElse(x.dateRead).getOrElse(LocalDate.now())

    super.read(path).map { lines =>
      (from, to) match
        case (None, None)           => lines
        case (Some(from), None)     => lines.filterNot(x => getDate(x).isBefore(from))
        case (None, Some(to))       => lines.filterNot(x => getDate(x).isAfter(to))
        case (Some(from), Some(to)) => lines.filterNot(x => getDate(x).isBefore(from) || getDate(x).isAfter(to))
    }

  def parse(line: Seq[String], header: Map[String, Int]): Either[Exception, Book] =
    val getRow = (cols: Seq[String], name: String) =>
      header.get(name) match
        case Some(index) => Try(line(index)).toOption
        case None        => None

    def parseTitle(title: String): (String, Option[String]) =
      val splits =
        if title.contains(":") then title.split(":", 2).toList
        else if title.contains(".") then title.split(".", 2).toList
        else List(title)

      splits.map(_.trim) match
        case title :: subtitle :: Nil => (title, Some(subtitle))
        case title :: Nil             => (title, None)
        case _                        => (title, None)

    val (title, subtitle) = parseTitle(getRow(line, "Title").getOrElse(""))
    try
      val book = Book(
        isbn13 = getRow(line, "ISBN13")
          .map(_.map(c => if c.isDigit then c else "").mkString(""))
          .flatMap(x => if x.isBlank then None else Some(x)),
        isbn10 = getRow(line, "ISBN")
          .map(_.map(c => if c.isDigit then c else "").mkString(""))
          .flatMap(x => if x.isBlank then None else Some(x)),
        googleId = None,
        goodreadsId = getRow(line, "Book Id"),
        title = title,
        subtitle = subtitle,
        author = getRow(line, "Author").getOrElse(""),
        additionalAuthors = getRow(line, "Additional Authors").map(_.split(",").toList.map(_.trim)),
        description = None,
        pageCount = getRow(line, "Number of Pages").flatMap(x => Try(x.toInt).toOption),
        publishedDate = None,
        yearPublished = getRow(line, "Year Published"),
        publisher = getRow(line, "Publisher"),
        language = None,
        categories = None,
        thumbnail = None,
        posterUrl = None,
        selfLink = None,
        previewLink = None,
        infoLink = None,
        goodreadsRating = getRow(line, "Average Rating").flatMap(_.toFloatOption),
        readStatus = getRow(line, "Exclusive Shelf").getOrElse("wishlist") match {
          case "currently-reading" => Some(ReadState.CurrentlyReading)
          case "read"              => Some(ReadState.Read)
          case "to-read"           => Some(ReadState.Wishlist)
          case _                   => None
        },
        dateAdded = getRow(line, "Date Added").flatMap(x => tryParseDate(x)),
        dateRead = getRow(line, "Date Read").flatMap(x => tryParseDate(x)),
        dateRated = getRow(line, "Date Read").flatMap(x => tryParseDate(x)),
        personalRating = getRow(line, "My Rating").flatMap(x => if x == "0" then None else Try(x.toInt).toOption),
      )
      Right(
        book.copy(posterUrl =
          book.isbn13
            .orElse(book.isbn10)
            .map(isbn => s"https://covers.openlibrary.org/b/isbn/$isbn-M.jpg")
        )
      )
    catch
      case e: Exception =>
        Left(new Exception(s"Error parsing line: $line, error: $e"))

  private def tryParseDate(
      raw: String,
      fmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
  ): Option[LocalDate] =
    Try(LocalDate.parse(raw, fmt)).toOption
