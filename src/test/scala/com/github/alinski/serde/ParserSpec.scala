package com.github.alinski.serde

import munit.FunSuite
// import com.github.alinski.service.GoodreadsCsvReaderService

class ParserSpec extends FunSuite:
  import ParsingUtils.*

  test("camelToSnake should convert camel case to snake case correctly") {
    assertEquals(camelToSnake("TheMovieDB"), "the_movie_db")
    assertEquals(camelToSnake("CamelCase"), "camel_case")
    assertEquals(camelToSnake("camelCase"), "camel_case")
    assertEquals(camelToSnake("HTTPResponseCode"), "http_response_code")
  }

  test("snakeToCamel should convert snake case to camel case correctly") {
    assertEquals(snakeToCamel("the_movie_db"), "theMovieDb")
    assertEquals(snakeToCamel("camel_case"), "camelCase")
    // assertEquals(snakeToCamel("aa_AAA"), "aaAaa")
  }
