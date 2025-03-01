package com.github.alinski.cli

import caseapp.*

case class BookOptions(
    @Name("isbn")
    isbn: Option[String] = None,
    @Name("goodreads-csv-file")
    goodreadsCSVFile: Option[String] = None,
    @Name("title")
    title: Option[String] = None,
    @Name("author")
    author: Option[String] = None,
    @Name("f")
    @Name("output-format")
    outputFormat: Option[String] = None,
    @Name("o")
    @Name("output-file")
    outputFile: Option[String] = None,
    @Name("n")
    @Name("limit")
    limit: Int = 5
)

case class VisualMediaOptions(
    @Name("imdb-id")
    id: Option[String] = None,
    @Name("imdb-csv-file")
    imdbCSVFile: Option[String] = None,
    @Name("f")
    @Name("output-format")
    outputFormat: Option[String] = None,
    @Name("o")
    @Name("output-file")
    outputFile: Option[String] = None,
    @Name("n")
    @Name("limit")
    limit: Int = 5
)
