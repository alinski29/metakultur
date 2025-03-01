package com.github.alinski.service

import com.github.alinski.io.{ApiClient, CollectionEndpoint, JsonEndpoint, SingleResourceEndpoint}
import com.github.alinski.model.VisualMedia
import sttp.client4.Response
import sttp.model.QueryParams
import sttp.model.Uri.PathSegment
import upickle.default.*

object MovieApiService:
  lazy private val getMovieByIdEndpoint: SingleResourceEndpoint = new JsonEndpoint with SingleResourceEndpoint:
    override def apiClient: ApiClient[VisualMedia] = TheMovieDbApiClient
    override def fetchById(id: String): Either[Response[String], Response[ujson.Value]] =
      getResponse(
        endpoint = apiClient.baseUri,
        pathSegments = List("3", "movie", id).map(PathSegment(_))
      )

  lazy private val searchMovieEndpoint: CollectionEndpoint = new JsonEndpoint with CollectionEndpoint:
    override def apiClient: ApiClient[VisualMedia] = TheMovieDbApiClient
    override def search(
        query: String,
        limit: Option[Int] = None
    ): Either[Response[String], Response[ujson.Value]] =
      getResponse(
        endpoint = apiClient.baseUri,
        pathSegments = List("3", "search", "movie").map(PathSegment(_)),
        queryParams = QueryParams.fromMap(Map("query" -> query))
      )

case class MovieApiService(
    getByIdEndpoint: SingleResourceEndpoint = getMovieByIdEndpoint,
    getByImdbIdEndpoint: SingleResourceEndpoint = TheMovieDbApiClient.getByImdbIdEndpoint,
    searchEndpoint: CollectionEndpoint = searchMovieEndpoint
) extends VisualMediaApiService
