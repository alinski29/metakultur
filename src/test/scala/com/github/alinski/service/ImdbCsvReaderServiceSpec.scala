package com.github.alinski.service

import munit.{FunSuite, Location}

class ImdbCsvReaderServiceSpec extends FunSuite:
  val dataPath    = os.pwd / "src" / "test" / "resources" / "csv_files" / "imdb_export.csv"
  val service     = ImdbCsvReaderService
  lazy val header = service.readHeader(dataPath)

  test("imdb CSV export: should parse all movies from the file") {
    service.read(dataPath) match
      case Right(movies) =>
        assert(movies.size == 6)
      case Left(err) =>
        fail(s"Expected file reading to succeed, but got error: $err")
  }

  test("imdb CSV export: should correcty read CSV header") {
    service.readHeader(dataPath) match
      case Right(header) =>
        assert(header("Title") == 3)
        assert(header("Your Rating") == 1)
      case Left(err) =>
        fail(s"Expected header reading to succeed, but got error: $err")
  }
