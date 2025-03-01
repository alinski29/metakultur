package com.github.alinski.service

import com.github.alinski.io.{ApiClient, CollectionEndpoint, JsonEndpoint, SingleResourceEndpoint}
import com.github.alinski.model.{VisualMedia, VisualMediaGenre, VisualMediaType}
import com.github.alinski.service.TheMovieDbApiClient.{baseUri, defaultHeaders}
import munit.FunSuite
import sttp.client4.Response
import sttp.model.Uri.UriContext
import sttp.model.*

class MovieApiServiceSpec extends FunSuite:
  import MovieApiServiceSpec.*

  test("Movie retrieval by imdb id, 200 response") {
    movieService.getByImdbId(movieImdbId) match
      case Right(movie) =>
        assert(movie.`type` == VisualMediaType.Movie)
        assert(movie.title == "Nosferatu")
        assert(movie.originalLanguage.contains("en"))
        assert(movie.dateReleased.contains(java.time.LocalDate.of(2024, 12, 25)))
        assert(movie.posterUrl.contains("https://image.tmdb.org/t/p/original/5qGIxdEO841C0tdY8vOdLoRVrr0.jpg"))
        assert(movie.imdbId == movieImdbId)
        assert(movie.tmdbId.get != movieImdbId)
        assert(movie.genres.get == List(VisualMediaGenre.Horror, VisualMediaGenre.Fantasy))

      case Left(err) =>
        fail(s"Expected retrieval to succeed, but got error: $err")
  }

  test("Movie retrieval by imdb id with empty lists should return DecodingFailure") {
    movieService.getByImdbId("empty") match
      case Left(err: ujson.ParseException) =>
        assert(err.getMessage.contains("Empty movie_results and tv_results"))
      case _ =>
        fail("Expected a DecodingFailure due to empty JSON lists")
  }

  test("Movie retrieval with missing required fields should return DecodingFailure") {
    movieService.getByImdbId("missingTitle") match
      case Left(err) =>
        assert(err.getMessage.contains("No title found"))
      case Right(value) =>
        fail("Expected a DecodingFailure due to missing required fields")
  }

  test("Movie retrieval with string id should succeed") {
    movieService.getByImdbId("idAsString") match
      case Right(movie) =>
        assert(movie.tmdbId.contains("426063"))
        assert(movie.imdbId == movieImdbId)
      case Left(err) =>
        fail(s"Expected retrieval to succeed, but got error: $err")
  }

  test("Movie retrieval with null fields should be handled gracefully") {
    movieService.getByImdbId("nullFields") match
      case Right(movie) =>
        assert(movie.originalTitle.isEmpty)
        assert(movie.posterUrl.isEmpty)
      case Left(err) =>
        fail(s"Expected retrieval to succeed with null fields, but got error: $err")
  }

  test("Movie from TheMovieDb retrieval for Movie by tmdb id") {
    movieService.getById(movieTmdbId) match
      case Right(movie) =>
        assert(movie.`type` == VisualMediaType.Movie)
        assert(movie.title == "Nosferatu")
        assert(movie.originalLanguage.contains("en"))
        assert(movie.dateReleased.contains(java.time.LocalDate.of(2024, 12, 25)))
        assert(movie.posterUrl.contains("https://image.tmdb.org/t/p/original/5qGIxdEO841C0tdY8vOdLoRVrr0.jpg"))
        assert(movie.imdbId == movieImdbId)
        assert(movie.tmdbRating.nonEmpty)
        assert(movie.tmdbId.contains(movieTmdbId))
        assert((movie.genres.get diff List(VisualMediaGenre.Horror, VisualMediaGenre.Fantasy)).isEmpty)
      case Left(err) =>
        fail(s"Expected retrieval to succeed, but got error: $err")
  }

  test("Movie search, 200 non-empty response") {
    movieService.search("Nosferatu") match
      case Right(movies) =>
        assert(movies.size == 4)
        assert(movies.head.title == "Nosferatu")
        assert(movies.head.tmdbId.contains(movieTmdbId))
        assert(movies.head.`type` == VisualMediaType.Movie)
      case Left(err) =>
        fail(s"Expected movie search to succeed, but got error: $err")
  }

  test("Movie search, 200 empty response") {
    movieService.search("empty") match
      case Right(movies) =>
        assert(movies.isEmpty)
      case Left(err) =>
        fail(s"Expected movie search to succeed, but got error: $err")
  }

object MovieApiServiceSpec:
  val apiResponsesPath = os.pwd / "src" / "test" / "resources" / "api_responses" / "themoviedb" / "movie"
  val movieImdbId      = "tt5040012" // Nosferatu
  val movieTmdbId      = "426063"

  val getByImdbIdEndpoint: SingleResourceEndpoint = new SingleResourceEndpoint with JsonEndpoint:
    override def apiClient: ApiClient[VisualMedia] = new ApiClient[VisualMedia]:
      val baseUri = uri"example.com"

    override def fetchById(id: String): Either[Response[String], Response[ujson.Value]] =
      def getRequestMeta(imdbId: String) =
        val params    = QueryParams.fromMap(Map("external_source" -> "imdb_id"))
        val targetUri = baseUri.copy().addPath("movie").addPath(imdbId).addParams(params)
        RequestMetadata(Method("GET"), targetUri, defaultHeaders)

      id match
        case `movieImdbId` =>
          val json = ujson.read(os.read(apiResponsesPath / "get_by_imdb_id_200.json"))
          Right(Response.ok(json, getRequestMeta(movieImdbId)))
        // emtpy response
        case "empty" =>
          val text = os.read(apiResponsesPath / "get_by_imdb_id_empty.json")
          val json = ujson.read(text)
          Right(Response.ok(json, getRequestMeta("empty")))
        case "missingTitle" =>
          val text         = os.read(apiResponsesPath / "get_by_imdb_id_200.json")
          val textModified = text.replaceAll("\"title\":\\s*\"Nosferatu\",\\s*", "")
          val json         = ujson.read(textModified)
          Right(Response.ok(json, getRequestMeta("missingTitle")))
        case "idAsString" =>
          val text         = os.read(apiResponsesPath / "get_by_imdb_id_200.json")
          val textModified = text.replaceAll("\"id\":\\s*([0-9]+)", "\"id\": \"$1\"")
          val json         = ujson.read(textModified)
          Right(Response.ok(json, getRequestMeta(movieImdbId)))
        case "nullFields" =>
          val text = os.read(apiResponsesPath / "get_by_imdb_id_200.json")
          val textModified = text
            .replaceAll("\"original_title\":\\s*\"[^\"]+\",\\s*", "\"original_title\": null, ")
            .replaceAll("\"poster_path\":\\s*\"[^\"]+\",\\s*", "\"poster_path\": null, ")
          val json = ujson.read(textModified)
          Right(Response.ok(json, getRequestMeta(movieImdbId)))
        case _ =>
          Left(Response("Not found", StatusCode(404), RequestMetadata(Method("GET"), baseUri, defaultHeaders)))

  private def getRequestMeta(id: String) =
    val targetUri = baseUri.copy().addPath("movie").addPath(id)
    RequestMetadata(Method("GET"), targetUri, defaultHeaders)

  val getByIdEndpoint: SingleResourceEndpoint = new SingleResourceEndpoint with JsonEndpoint:
    override def apiClient: ApiClient[VisualMedia] = new ApiClient[VisualMedia]:
      val baseUri = uri"example.com"

    override def fetchById(id: String): Either[Response[String], Response[ujson.Value]] =
      id match
        case `movieTmdbId` =>
          val json = ujson.read(os.read(apiResponsesPath / "get_by_tmdb_id_200.json"))
          Right(Response.ok(json, getRequestMeta("426063")))
        case id =>
          Left(Response("Not found", StatusCode(404), getRequestMeta(id)))

  val searchEndpoint: CollectionEndpoint = new CollectionEndpoint with JsonEndpoint:
    override def apiClient: ApiClient[VisualMedia] = new ApiClient[VisualMedia]:
      val baseUri = uri"example.com"

    override def search(
        query: String,
        limit: Option[Int] = None
    ): Either[Response[String], Response[ujson.Value]] =
      query match
        case q if q == "Nosferatu" =>
          val json = ujson.read(os.read(apiResponsesPath / "search_200.json"))
          Right(Response.ok(json, getRequestMeta(query)))
        case q if q == "empty" =>
          val json = ujson.read("""{"page":1,"results":[],"total_pages":1,"total_results":0}""")
          Right(Response.ok(json, getRequestMeta(query)))
        case q =>
          Left(Response("Not found", StatusCode(404), getRequestMeta(q)))

  val movieService = MovieApiService(
    getByIdEndpoint = getByIdEndpoint,
    getByImdbIdEndpoint = getByImdbIdEndpoint,
    searchEndpoint = searchEndpoint
  )
