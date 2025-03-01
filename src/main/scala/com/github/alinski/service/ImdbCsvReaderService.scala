package com.github.alinski.service

import com.github.alinski.io.CsvReader
import com.github.alinski.model.{VisualMedia, VisualMediaGenre, VisualMediaType}

import scala.concurrent.duration.Duration
import scala.util.Try

object ImdbCsvReaderService extends CsvReader[VisualMedia]:

  def parse(line: Seq[String], header: Map[String, Int]): Either[Exception, VisualMedia] =
    val getRow = (cols: Seq[String], name: String) =>
      header.get(name) match
        case Some(index) => Try(line(index)).toOption
        case None        => None

    try
      val movie = VisualMedia(
        tmdbId = None,
        imdbId = getRow(line, "Const").getOrElse(""),
        title = getRow(line, "Title").getOrElse(""),
        originalTitle = getRow(line, "Original Title"),
        `type` = getRow(line, "Title Type") match {
          case Some("Movie")          => VisualMediaType.Movie
          case Some("TV Series")      => VisualMediaType.TVSeries
          case Some("TV Mini Series") => VisualMediaType.TVSeries
          case _                      => VisualMediaType.Movie
        },
        imdbRating = getRow(line, "IMDb Rating").flatMap(_.toDoubleOption.map(_.toFloat)),
        runtime = getRow(line, "Runtime (mins)").map(x => Duration(x + " minutes")),
        year = getRow(line, "Year"),
        dateReleased = getRow(line, "Release Date").map(x => java.time.LocalDate.parse(x)),
        genres =
          getRow(line, "Genres").map(x => x.split(",").toList.flatMap(g => VisualMediaGenre.fromString(g.trim))),
        directors = getRow(line, "Directors").map(x => x.split(",").map(_.trim).toList).getOrElse(List()),
        description = None,
        originalLanguage = None,
      )
      Right(movie)
    catch
      case e: Exception =>
        Left(new Exception(s"Error parsing line: $line, error: $e"))
