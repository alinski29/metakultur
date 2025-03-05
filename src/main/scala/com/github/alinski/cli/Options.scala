package com.github.alinski.cli

import caseapp.*

case class SharedOptions(
    @HelpMessage(s"One of: json, yaml, md. (default: json)")
    @Name("f")
    outputFormat: Option[String] = None,
    @HelpMessage("Path to output file")
    @Name("o")
    output: Option[String] = None,
    @HelpMessage("Number of max results returned (default: 5)")
    @Name("n")
    limit: Int = 5,
    @HelpMessage("Add personal rating details")
    rate: Boolean = false,
    @HelpMessage(
      "Filter the imdb or goodreads CSV with records starting from this date. Required format: yyyy-MM-dd"
    )
    from: Option[String] = None,
    @HelpMessage("Filter the imdb or goodreads CSV with records up to this date. Required format: yyyy-MM-dd")
    to: Option[String] = None,
    @HelpMessage("Enrich the CSV file with additional details fetched from API: may require an API key")
    enrich: Boolean = false
)

case class BookOptions(
    @HelpMessage("Book isbn13 or isbn10")
    isbn: Option[String] = None,
    @HelpMessage("Book title to search for")
    title: Option[String] = None,
    @HelpMessage("Book author to search for")
    author: Option[String] = None,
    @HelpMessage("Path to CSV export from Goodreads")
    goodreadsCSVFile: Option[String] = None,
    @Recurse
    shared: SharedOptions = SharedOptions()
)

case class VisualMediaOptions(
    @HelpMessage("IMDB id")
    imdbId: Option[String] = None,
    @HelpMessage("TMDB (The Movie Database) id")
    tmdbId: Option[String] = None,
    @HelpMessage("Path to CSV export from IMDB")
    imdbCSVFile: Option[String] = None,
    @Recurse
    shared: SharedOptions = SharedOptions()
)
