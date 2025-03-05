package com.github.alinski.service

import com.github.alinski.io.{ApiClient, JsonEndpoint, SingleResourceEndpoint}
import com.github.alinski.model.Book
import sttp.client4.{Response, UriContext}
import sttp.model.Uri.PathSegment
import sttp.model.{Header, QueryParams, Uri}
import sttp.client4.quick.basicRequest
import sttp.model.StatusCode

object GoogleBooksApiClient extends ApiClient[Book]:
  val baseUri = uri"https://www.googleapis.com/books/v1"
  override val defaultHeaders =
    sys.env.get("GOOGLE_BOOKS_TOKEN") match
      case Some(token) if token.nonEmpty => super.defaultHeaders :+ Header("key", token)
      case _                             => super.defaultHeaders

  // def checkImage(url: Uri): Boolean =
  //   val request = basicRequest
  //     .get(url)
  //     .headers(basicRequest.headers)

  //   val response = request.send(super.backend)

  //   response.body match
  //     case Left(value)  => false
  //     case Right(value) => value.size > 0 && response.statusCode == StatusCode(200)
