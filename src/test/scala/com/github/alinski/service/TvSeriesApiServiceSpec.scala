package com.github.alinski.service

import com.github.alinski.io.{ApiClient, CollectionEndpoint, JsonEndpoint, SingleResourceEndpoint}
import com.github.alinski.model.{VisualMedia, VisualMediaGenre, VisualMediaType}
import com.github.alinski.service.TheMovieDbApiClient.{baseUri, defaultHeaders}
import munit.FunSuite
import sttp.client4.Response
import sttp.model.Uri.UriContext
import sttp.model.*

class TvSeriesApiServiceSpec extends FunSuite:
  import TvSeriesApiServiceSpec.*

  test("TV series retrieval by imdb id, 200 response") {
    tvService.getByImdbId(tvSeriesImdbId) match
      case Right(movie) =>
        assert(movie.`type` == VisualMediaType.TVSeries)
        assert(movie.title == "The Last of Us")
        assert(movie.originalLanguage.contains("en"))
        assert(movie.dateReleased.contains(java.time.LocalDate.of(2023, 1, 15)))
        assert(movie.originCountry.contains("US"))
        assert(movie.genres.nonEmpty)
        assert(movie.imdbId == tvSeriesImdbId)
        assert(movie.tmdbId.get != tvSeriesImdbId)
      case Left(err) =>
        fail(s"Expected retrieval to succeed, but got error: $err")
  }

  test("TV Series retrieval by tmdb id") {
    tvService.getById(tvSeriesTmdbId) match
      case Right(movie) =>
        assert(movie.`type` == VisualMediaType.TVSeries)
        assert(movie.title == "The Last of Us")
        assert(movie.originalLanguage.contains("en"))
        assert(movie.dateReleased.contains(java.time.LocalDate.of(2023, 1, 15)))
        assert(movie.originCountry.contains("US"))
        assert((movie.genres.get diff List(VisualMediaGenre.Drama)).isEmpty)
        // TODO: Imdb ID is not avaialble for TV series
        // assert(movie.imdbId == tvSeriesImdbId)
        assert(movie.tmdbId.contains(tvSeriesTmdbId))
      case Left(err) =>
        fail(s"Expected retrieval to succeed, but got error: $err")
  }

  test("TV search, 200 non-empty response") {
    tvService.search("The Last of Us") match
      case Right(movies) =>
        assert(movies.size == 3)
        assert(movies.head.title == "The Last of Us")
        assert(movies.head.tmdbId.contains(tvSeriesTmdbId))
        assert(movies.head.`type` == VisualMediaType.TVSeries)
      case Left(err) =>
        fail(s"Expected TV series search to succeed, but got error: $err")
  }

  test("TV search, 200 empty response") {
    tvService.search("empty") match
      case Right(movies) =>
        assert(movies.isEmpty)
      case Left(err) =>
        fail(s"Expected TV series search to succeed, but got error: $err")
  }

object TvSeriesApiServiceSpec:
  val apiResponsesPath = os.pwd / "src" / "test" / "resources" / "api_responses" / "themoviedb" / "tv"
  val tvSeriesImdbId   = "tt3581920" // The Last of us
  val tvSeriesTmdbId   = "100088"

  val getByImdbIdEndpoint: SingleResourceEndpoint = new SingleResourceEndpoint with JsonEndpoint:
    override def apiClient: ApiClient[VisualMedia] = new ApiClient[VisualMedia]:
      val baseUri = uri"example.com"

    override def fetchById(id: String): Either[Response[String], Response[ujson.Value]] =
      def getRequestMeta(imdbId: String) =
        val params    = QueryParams.fromMap(Map("external_source" -> "imdb_id"))
        val targetUri = baseUri.copy().addPath("tv").addPath(imdbId).addParams(params)
        RequestMetadata(Method("GET"), targetUri, defaultHeaders)

      id match
        case `tvSeriesImdbId` =>
          val json = ujson.read(os.read(apiResponsesPath / "get_by_imdb_id_200.json"))
          Right(Response.ok(json, getRequestMeta(tvSeriesImdbId)))
        // emtpy response
        case _ =>
          Left(Response("Not found", StatusCode(404), RequestMetadata(Method("GET"), baseUri, defaultHeaders)))

  val getByIdEndpoint: SingleResourceEndpoint = new SingleResourceEndpoint with JsonEndpoint:
    override def apiClient: ApiClient[VisualMedia] = new ApiClient[VisualMedia]:
      val baseUri = uri"example.com"

    override def fetchById(id: String): Either[Response[String], Response[ujson.Value]] =
      def getRequestMeta(id: String) =
        val targetUri = baseUri.copy().addPath("tv").addPath(id)
        RequestMetadata(Method("GET"), targetUri, defaultHeaders)

      id match
        case id if id == tvSeriesTmdbId =>
          val json = ujson.read(os.read(apiResponsesPath / "get_by_tmdb_id_200.json"))
          Right(Response.ok(json, getRequestMeta(tvSeriesTmdbId)))
        case id =>
          Left(Response("Not found", StatusCode(404), getRequestMeta(id)))

  val searchEndpoint: CollectionEndpoint = new CollectionEndpoint with JsonEndpoint:
    override def apiClient: ApiClient[VisualMedia] = new ApiClient[VisualMedia]:
      val baseUri = uri"example.com"

    override def search(
        query: String,
        limit: Option[Int] = None
    ): Either[Response[String], Response[ujson.Value]] =
      def getRequestMeta(id: String) =
        val targetUri = baseUri.copy().addPath("tv").addPath(id)
        RequestMetadata(Method("GET"), targetUri, defaultHeaders)

      query match
        case q if q == "The Last of Us" =>
          val json = ujson.read(os.read(apiResponsesPath / "search_200.json"))
          Right(Response.ok(json, getRequestMeta(query)))
        case q if q == "empty" =>
          val json = ujson.read("""{"page":1,"results":[],"total_pages":1,"total_results":0}""")
          Right(Response.ok(json, getRequestMeta(query)))
        case q =>
          Left(Response("Not found", StatusCode(404), getRequestMeta(q)))

  val tvService = TvSeriesApiService(
    getByIdEndpoint = getByIdEndpoint,
    getByImdbIdEndpoint = getByImdbIdEndpoint,
    searchEndpoint = searchEndpoint
  )
