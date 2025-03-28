package com.github.alinski.service

import com.github.alinski.io.{ApiClient, JsonEndpoint, SingleResourceEndpoint}
import com.github.alinski.model.VisualMedia
import sttp.client4.{Response, UriContext}
import sttp.model.Uri.PathSegment
import sttp.model.{Header, QueryParams}

object TheMovieDbApiClient extends ApiClient[VisualMedia]:
  val baseUri = uri"https://api.themoviedb.org"
  override val defaultHeaders =
    sys.env.get("THEMOVIEDB_TOKEN") match
      case Some(token) if token.nonEmpty => super.defaultHeaders :+ Header("Authorization", s"Bearer $token")
      case _                             => super.defaultHeaders

  lazy val getByImdbIdEndpoint: SingleResourceEndpoint = new JsonEndpoint with SingleResourceEndpoint:
    override def apiClient: ApiClient[VisualMedia] = TheMovieDbApiClient
    override def fetchById(id: String): Either[Response[String], Response[ujson.Value]] =
      getResponse(
        endpoint = apiClient.baseUri,
        pathSegments = List("3", "find", id).map(PathSegment(_)),
        queryParams = QueryParams.fromMap(Map("external_source" -> "imdb_id"))
      )
