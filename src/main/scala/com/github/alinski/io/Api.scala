package com.github.alinski.io

import sttp.client4.*
import sttp.client4.curl.CurlBackend
import sttp.client4.quick.basicRequest
import sttp.model.Uri.Segment
import sttp.model.{Header, QueryParams, Uri}

trait ApiClient[T]:
  val backend: SyncBackend        = CurlBackend()
  def defaultHeaders: Seq[Header] = basicRequest.headers
  def baseUri: Uri

trait JsonEndpoint:
  def apiClient: ApiClient[_]

  def getResponse(
      endpoint: Uri,
      pathSegments: Seq[Segment] = Seq.empty[Segment],
      queryParams: QueryParams = QueryParams(),
      headers: Seq[Header] = Seq.empty[Header]
  ): Either[Response[String], Response[ujson.Value]] =

    val targetUri = apiClient.baseUri.addPathSegments(pathSegments).addParams(queryParams)
    val request = basicRequest
      .get(targetUri)
      .headers(apiClient.defaultHeaders ++ headers: _*)
      .mapResponse(_.map(ujson.read(_)))
    // .mapResponse(x => x.map(z => Try(JsonParser.parse(z)).toOption.get))

    val response: Response[Either[String, ujson.Value]] = request.send(apiClient.backend)

    response.body match
      case Left(value)  => Left(response.copy[String](body = value))
      case Right(value) => Right(response.copy[ujson.Value](body = value))

trait SingleResourceEndpoint:
  self: JsonEndpoint =>
  def fetchById(id: String): Either[Response[String], Response[ujson.Value]]

trait CollectionEndpoint:
  self: JsonEndpoint =>
  def search(query: String, limit: Option[Int] = None): Either[Response[String], Response[ujson.Value]]
