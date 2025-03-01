package com.github.alinski.service

import com.github.alinski.io.{CollectionEndpoint, SingleResourceEndpoint}
import com.github.alinski.model.{VisualMedia, VisualMediaGenre, VisualMediaType}
import sttp.client4.Response
import upickle.default.*

import java.time.LocalDate
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

//sealed trait ApiService[T]:
//  def getById(id: String): Either[Exception, T]
//
//  def search(query: String, limit: Int): Either[Exception, LazyList[T]]

trait VisualMediaApiService:
  val getByIdEndpoint: SingleResourceEndpoint
  def getByImdbIdEndpoint: SingleResourceEndpoint
  def searchEndpoint: CollectionEndpoint

  import com.github.alinski.serde.ParsingUtils.jsonExtensions.*

  def getById(id: String): Either[Exception, VisualMedia] =
    getByIdEndpoint.fetchById(id) match
      case Left(err) =>
        Left(ujson.ParseException(s"Invalid ujson.Value response, error: $err", 0))
      case Right(response: Response[ujson.Value]) =>
        parseMovieResult(response.body.obj)

  def search(query: String, limit: Int = 10): Either[Exception, LazyList[VisualMedia]] =
    searchEndpoint.search(query) match
      case Left(err) =>
        Left(ujson.ParseException("Invalid ujson.Value response", 0))
      case Right(response: Response[ujson.Value]) =>
        response.body.obj.get("results") match
          case None =>
            Left(ujson.ParseException("No results found", 0))
          case Some(results) =>
            results.arr
              .take(limit)
              .map(r => parseMovieResult(r.obj))
              .foldLeft(Right(LazyList.empty[VisualMedia]): Either[Exception, LazyList[VisualMedia]]) {
                case (Left(err), _)                => Left(err)
                case (_, Left(err))                => Left(err)
                case (Right(movies), Right(movie)) => Right(movies :+ movie)
              }

  def getByImdbId(id: String): Either[Exception, VisualMedia] =
    getByImdbIdEndpoint.fetchById(id) match
      case Left(err) =>
        Left(ujson.ParseException("Invalid ujson.Value response", 0))
      case Right(response: Response[ujson.Value]) =>
        val imdbId = response.request.uri.pathSegments.segments.lastOption.map(_.v)
        parseMovieResults(response.body.obj, Map("imdb_id" -> imdbId.getOrElse("")))

  private def parseMovieResult(
      record: upickle.core.LinkedHashMap[String, ujson.Value],
      extra: Map[String, String] = Map.empty[String, String],
      mediaType: VisualMediaType = VisualMediaType.Unknown,
  ): Either[Exception, VisualMedia] =
    if !record.hasField("title") && !record.hasField("name") then
      return Left(ujson.ParseException("No title found", 0))

    val mediaTypeFinal = mediaType match
      case VisualMediaType.Unknown if record.hasFields(Seq("title", "release_date"))  => VisualMediaType.Movie
      case VisualMediaType.Unknown if record.hasFields(Seq("name", "first_air_date")) => VisualMediaType.TVSeries
      case _                                                                          => mediaType
    // scribe.info(s"keys: ${record.keySet.mkString(", ")}")
    // val mediaTypeFinal =
    //   if record.hasFields(Seq("title", "release_date")) then VisualMediaType.Movie
    //   else if record.hasFields(Seq("name", "first_air_date")) then VisualMediaType.TVSeries
    //   else VisualMediaType.Unknown
    // scribe.info(s"mediaTypeFinal: $mediaTypeFinal")

    val genres =
      (record / "genre_ids")
        .map(_.arr.toList.flatMap(r => VisualMediaGenre.fromId(r.num.toInt)))
        .orElse(
          (record / "genres")
            .map(_.arr.toList.flatMap(js => js.obj.get("id").map(_.num.toInt).flatMap(VisualMediaGenre.fromId)))
        )

    try
      val movie = VisualMedia(
        tmdbId = (record / "id").map(x => x.numOpt.map(_.toInt.toString).orElse(x.strOpt).getOrElse("")),
        imdbId = (record / "imdb_id").map(_.str).orElse(extra.get("imdb_id")).getOrElse(""),
        title = (record / "title").orElse(record / "name").map(_.str).getOrElse(""),
        originalTitle = (record / "original_title").orElse(record / "original_name").map(_.str),
        originalLanguage = (record / "original_language").map(_.str),
        `type` = mediaTypeFinal,
        tmdbRating = record / "vote_average" map (_.num.toFloat),
        dateReleased = (record / "release_date").orElse(record / "first_air_date").map(_.str).map(LocalDate.parse),
        year = (record / "release_date").map(_.str).map(_.take(4)),
        directors = List.empty[String],
        genres = genres,
        runtime = (record / "runtime").map(_.num.toInt).map(d => Duration(d, "minutes")),
        description = record / "overview" map (_.str),
        posterUrl = (record / "poster_path").map(_.str).map(p => s"https://image.tmdb.org/t/p/original${p}"),
        originCountry = (record / "origin_country").flatMap(_.arr.toList.headOption.map(_.str)),
      )
      Right(movie)
    catch case e: Exception => Left(e)

  private def parseMovieResults(
      content: ujson.Value,
      extra: Map[String, String] = Map.empty[String, String]
  ): Either[Exception, VisualMedia] =
    Try(read[ujson.Value](content.toString)) match
      case Failure(ex: ujson.ParseException) => Left(ex)
      case Failure(ex)                       => Left(ujson.ParseException(ex.getMessage, 0))
      case Success(value) =>
        val obj            = value.obj
        val requiredFields = Seq("movie_results", "tv_results")

        if !obj.hasFields(requiredFields)
        then return Left(ujson.ParseException(s"Missing required fields: ${requiredFields.mkString(", ")}", 0))

        val maybeMediaType =
          if (obj / "movie_results").exists(_.arr.nonEmpty) then Some(VisualMediaType.Movie)
          else if (obj / "tv_results").exists(_.arr.nonEmpty) then Some(VisualMediaType.TVSeries)
          else None

        if maybeMediaType.isEmpty
        then return Left(ujson.ParseException("Empty movie_results and tv_results", 0))

        val record = maybeMediaType match
          case Some(VisualMediaType.Movie) => (obj / "movie_results").get.arr.head.obj
          case _                           => (obj / "tv_results").get.arr.head.obj

        parseMovieResult(record, extra, maybeMediaType.getOrElse(VisualMediaType.Unknown))
