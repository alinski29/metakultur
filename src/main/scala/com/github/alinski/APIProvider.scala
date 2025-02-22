package com.github.alinski

import io.circe.{Json, Decoder, Encoder, HCursor, ACursor}
import io.circe.generic.auto.*
import io.circe.generic.semiauto.*
import io.circe.parser.*
import io.circe.syntax.*

import sttp.client4.*
import sttp.client4.circe.*
import sttp.client4.curl.CurlBackend
import sttp.client4.quick.basicRequest
import sttp.client4.quickRequest.*
import sttp.model.{Header, Uri, QueryParams}

import scala.util.Try
import sttp.model.Uri.PathSegment
import sttp.model.Uri.Segment
import io.circe.ParsingFailure
import io.circe.Decoder

import scala.concurrent.duration.Duration
import java.time.{Instant, LocalDate, ZoneId}
import io.circe.DecodingFailure

trait APIProvider[T] {

  val backend: SyncBackend = CurlBackend()

  def defaultHeaders: Seq[Header] = basicRequest.headers

  def baseUri: Uri

  def getById(id: String): Either[Response[String], Response[T]]
}

// Wrapper(Response[JSON], Either[ParsingFailure, Movie] )

object APIProvider:

  trait JsonAPIProvider extends APIProvider[Json]:
    def getResponse(
        endpoint: Uri,
        pathSegments: Seq[Segment] = Seq.empty[Segment],
        queryParams: QueryParams = QueryParams(),
        headers: Seq[Header] = Seq.empty[Header]
    ): Either[Response[String], Response[Json]] =
      val targetUri = baseUri.addPathSegments(pathSegments).addParams(queryParams)
      val request = basicRequest
        .get(targetUri)
        .headers(defaultHeaders ++ headers: _*)
        .mapResponse(_.map(x => parse(x).getOrElse(Json.Null)))

      val response: Response[Either[String, Json]] = request.send(backend)

      response.body match
        case Left(value)  => Left(response.copy[String](body = value))
        case Right(value) => Right(response.copy[Json](body = value))

  trait ContentSerializer[A, B, E]:
    def serialize(content: A): Either[Exception, B]

  trait JsonContentSerializer[B] extends ContentSerializer[Json, B, DecodingFailure]:
    def serialize(content: Json): Either[DecodingFailure, B]

  object ContentSerializer:

    object TheMovieDBContentSerializer extends JsonContentSerializer[Movie]:

      private final case class MovieResponse(
          backdropPath: String,
          id: Int,
          title: String,
          originalTitle: String,
          overview: String,
          posterPath: String,
          mediaType: String,
          adult: Boolean,
          originalLanguage: String,
          genreIds: List[Int],
          popularity: Double,
          releaseDate: String,
          video: Boolean,
          voteAverage: Double,
          voteCount: Int
      )

      private final case class MovieFindResponse(
          movieResults: List[MovieResponse],
          personResults: List[String],
          tvResults: List[String],
          tvEpisodeResults: List[String],
          tvSeasonResults: List[String]
      )

      def serialize(content: Json): Either[DecodingFailure, Movie] = content.as[MovieFindResponse] match
        case Right(value) =>
          val movie = value.movieResults.head
          Right(
            Movie(
              imdbId = movie.id.toString,
              title = movie.title,
              originalTitle = movie.originalTitle,
              `type` = MediaType.Movie,
              imdbRating = movie.voteAverage,
              dateReleased = LocalDate.parse(movie.releaseDate),
              year = movie.releaseDate.take(4),
              directors = List.empty[String],
              genres = List.empty[String],
              description = Some(movie.overview),
              posterUrl = Some(s"https://image.tmdb.org/t/p/original${movie.posterPath}")
            )
          )
        case Left(e) => Left(e)



    // val decoder = new Decoder[Movie]:
    //   final def apply(c: HCursor): Decoder.Result[Movie] =
    //     for
    //       id <- c.downField("id").as[Int]
    //       title <- c.downField("title").as[String]
    //       originalTitle <- c.downField("original_title").as[String]
    //       `type` <- c.downField("media_type").as[String]
    //       imdbRating <- c.downField("vote_average").as[Double]
    //       runtime <- c.downField("runtime").as[Int]
    //       dateReleased <- c.downField("release_date").as[String]
    //       year <- c.downField("release_date").as[String]
    //       directors <- c.downField("directors").as[List[String]]
    //       genres <- c.downField("genres").as[List[String]]
    //     yield Movie(
    //       id = id,
    //       title = title,
    //       originalTitle = originalTitle,
    //       `type` = `type`,
    //       imdbRating = imdbRating,
    //       runtime = Duration.ofMinutes(runtime),
    //       dateReleased = LocalDate.parse(dateReleased),
    //       year = year,
    //       directors = directors,
    //       genres = genres
    //     )

    // private given dailySleepDecoder: Decoder[DailySleep] = new Decoder[DailySleep]:
    //   final def apply(c: HCursor): Decoder.Result[DailySleep] =
    //     val sleep = c.downField("dailySleepDTO")


    // response

    // response.map(json => Movie.fromJSON(json))

  object TheMovieDBJsonAPIProvider extends JsonAPIProvider:
    val baseUri = uri"https://api.movies.com"

    def getById(id: String): Either[Response[String], Response[Json]] =
      getResponse(
        endpoint = baseUri,
        pathSegments = List("3", "find", id).map(PathSegment(_)),
        queryParams = QueryParams.fromMap(Map("external_source" -> "imdb_id"))
      )

// def search(query: String): Try[Json] =
//   getResponse(baseUri.addPathSegments(Segment("movies"), Segment("search")), QueryParams("q" -> query)).body match
//     case Left(value)  => Failure(new Exception(s"API error: $value"))
//     case Right(value) => Success(value)

// class TheMovieDBProvider(apiToken: Option[String] = None) extends JsonAPIProvider:
// val baseUri = uri"https://api.themoviedb.org"

// def getJsonResponse(
//     endpoint: String,
//     pathSegments: Seq[Segment],
//     params: QueryParams
// ): Try[Json] =

//   val headers = basicRequest.headers ++
//     apiToken.map(token => Header("Authorization", s"Bearer $token"))

//   // sttp.model.Uri
//   // val uri = Uri(baseUrl)
//   val targetUri = baseUri.addParams(Map("external_source" -> "imdb_id"))
//   baseUri.addPathSegments(Segment("3"), Segment("find"), Segment("tt0111161"))

//   // uri.withParams

//   // val targetUrl = s"$baseUrl$path"
//   val request = basicRequest
//     .get(targetUri)
//     // .get(uri"$targetUrl")
//     .headers(headers: _*)
//     .mapResponse(_.map(x => parse(x).getOrElse(Json.Null)))

//   backend.send(request).body match
//     case Left(value)  => Failure(new Exception(s"API error: $value"))
//     case Right(value) => Success(value)

// def getById(id: String): Try[Json] =
//   getJsonResponse(s"/3/find/$id?external_source=imdb_id")

// given APIProvider[Movie] with {
//   def getById(id: String): Try[Movie] = Try {
//     val response = requests.get(s"https://api.movies.com/movies/$id")
//     if (response.statusCode == 200)
//       io.circe.parser.decode[Movie](response.text).getOrElse(throw new Exception("Decoding failed"))
//     else
//       throw new Exception(s"API error: ${response.statusCode}")
//   }

//   def search(query: String): Try[List[Movie]] = Try {
//     val response = requests.get(
//       s"https://api.movies.com/movies/search",
//       params = Map("q" -> query, "limit" -> "10")
//     )
//     if (response.statusCode == 200)
//       io.circe.parser
//         .decode[List[Movie]](response.text)
//         .getOrElse(throw new Exception("Decoding failed"))
//     else
//       throw new Exception(s"API error: ${response.statusCode}")
//   }
// }

// given APIProvider[Book] with {
//   def getById(id: String): Try[Book] = Try {
//     val response = requests.get(s"https://api.books.com/books/$id")
//     if (response.statusCode == 200)
//       io.circe.parser.decode[Book](response.text).getOrElse(throw new Exception("Decoding failed"))
//     else
//       throw new Exception(s"API error: ${response.statusCode}")
//   }

//   def search(query: String): Try[List[Book]] = Try {
//     val response = requests.get(
//       s"https://api.books.com/books/search",
//       params = Map("q" -> query, "limit" -> "10")
//     )
//     if (response.statusCode == 200)
//       io.circe.parser
//         .decode[List[Book]](response.text)
//         .getOrElse(throw new Exception("Decoding failed"))
//     else
//       throw new Exception(s"API error: ${response.statusCode}")
//   }
// }
