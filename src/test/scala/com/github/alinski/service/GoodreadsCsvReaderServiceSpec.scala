package com.github.alinski.service

import munit.{FunSuite, Location}
import com.github.alinski.model.ReadState

class GoodreadsCsvReaderServiceSpec extends FunSuite:
  val dataPath    = os.pwd / "src" / "test" / "resources" / "csv_files" / "goodreads_export.csv"
  val service     = GoodreadsCsvReaderService
  lazy val header = service.readHeader(dataPath)

  test("goodreads CSV export: should parse all books from the file") {
    service.read(dataPath) match
      case Right(books) =>
        assert(books.size == 4)
        val firstBook = books.head
        assert(firstBook.isbn13.contains("9781538719985"))
        assert(firstBook.readStatus.contains(ReadState.Read))
      case Left(err) =>
        fail(s"Expected file reading to succeed, but got error: $err")
  }

  test("goodreads CSV export: should correcty read CSV header") {
    service.readHeader(dataPath) match
      case Right(header) =>
        assert(header("Author") == 2)
        assert(header("ISBN") == 5)
        assert(header("Number of Pages") == 11)
      case Left(err) =>
        fail(s"Expected header reading to succeed, but got error: $err")
  }
