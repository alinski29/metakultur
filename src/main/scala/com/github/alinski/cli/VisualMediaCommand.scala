package com.github.alinski.cli

import caseapp.*
import com.github.alinski.io.{FileWriter, WriteOptions}
import com.github.alinski.model.VisualMedia
import com.github.alinski.serde.Serializer.MarkdownTable
import com.github.alinski.serde.{FileFormat, Serializer}
import com.github.alinski.service.{ImdbCsvReaderService, VisualMediaApiService}

import scala.annotation.tailrec

trait VisualMediaCommand extends Command[VisualMediaOptions]:
  def name: String
  def mediaService: VisualMediaApiService

  def run(opts: VisualMediaOptions, args: RemainingArgs): Unit =
    val maybeMovies = (opts.id, opts.imdbCSVFile) match
      case (Some(id), _)       => mediaService.getByImdbId(id).map(LazyList(_))
      case (_, Some(filePath)) => ImdbCsvReaderService.read(os.Path(filePath))
      case (None, None) if args.all.nonEmpty =>
        userPromptInteraction(mediaService, args.all.mkString(" "), opts.limit)
      case _ => Left(IllegalArgumentException("No id, file or search query provided. At least one is required"))

    val format           = resolveOutputFormat(opts.outputFormat, opts.outputFile)
    val moviesSerialized = maybeMovies.map(_.map(Serializer.serialize(_, format)))

    opts.outputFile.map(os.Path(_)) match
      case None =>
        moviesSerialized.foreach(_.foreach(println))
      case Some(path) =>
        val result = for
          movies <- moviesSerialized
          file   <- FileWriter.write(movies, path, WriteOptions.default, Some(format))
        yield file
        result match
          case Right(file) => scribe.info(s"Movies written to $file")
          case Left(e)     => throw e

  @tailrec
  private def userPromptInteraction(
      service: VisualMediaApiService,
      query: String,
      limit: Int
  ): Either[Exception, LazyList[VisualMedia]] =
    service.search(query, limit = limit) match
      case Left(err) => Left(err)
      case Right(movies) if movies.nonEmpty =>
        val selection = promptUserToSelectMovie(movies)
        val movie     = selection.flatMap(movie => service.getById(movie.head.tmdbId.get))
        movie.map(LazyList(_))
      case Right(movies) =>
        println(s"No results found for your search query: '$query'. Enter a new query")
        val newQuery = scala.io.StdIn.readLine()
        userPromptInteraction(service, newQuery, limit)

  private def promptUserToSelectMovie(movies: LazyList[VisualMedia]): Either[Exception, LazyList[VisualMedia]] =
    if movies.isEmpty then return Left(Exception("No movies found"))
    println("Select a movie by its index: \n")

    val headers = Seq("Index", "Title", "Year", "Lang", "Genres")
    val rows = movies.zipWithIndex.map { case (movie, index) =>
      val genres = movie.genres.getOrElse(List()).mkString(", ")
//      val rating   = movie.imdbRating.flatMap(x => if x > 0 then Some(x.toString) else None).getOrElse("")
      val language = movie.originalLanguage.getOrElse("")
      Seq(
        (index + 1).toString,
        movie.title,
        movie.year.getOrElse("N/A"),
        language,
//        rating,
        genres
      )
    }

    val markdownTable = MarkdownTable.serialize(rows, headers)
    println(markdownTable)

    val selectedIndex = scala.io.StdIn.readInt() - 1
    if selectedIndex < 0 || selectedIndex >= movies.size then Left(Exception("Invalid selection"))
    else Right(LazyList(movies(selectedIndex)))

  private def resolveOutputFormat(outputFormat: Option[String], outputFile: Option[String]): FileFormat =
    outputFormat
      .map(FileFormat(_))
      .orElse(outputFile.map(FileFormat(_)))
      .getOrElse(FileFormat.Json)
