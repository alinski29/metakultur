package com.github.alinski.serde

import com.github.alinski.service.ImdbCsvReaderService
import munit.{FunSuite, Location}

class SerializerSpec extends FunSuite:
  val dataPath = os.pwd / "src" / "test" / "resources" / "csv_files" / "imdb_export.csv"
  val service  = ImdbCsvReaderService

  import ParsingUtils.jsonExtensions.*

  test("movies should correctly serialize to json") {
    val data = service.read(dataPath)
    data match
      case Right(movies) =>
        val records = movies.map(Serializer.serialize(_, FileFormat.Json))
        assert(records.size == 6)
        for record <- records do
          JsonParser.parse(record) match
            case Right(json) =>
              assert(json.obj.hasFields(Seq("title", "imdb_id", "genres", "imdb_rating")))
            case Left(err) =>
              fail(s"Expected json parsing to succeed, but got error: $err")
      case Left(err) =>
        fail(s"Expected file reading to succeed, but got error: $err")
  }
