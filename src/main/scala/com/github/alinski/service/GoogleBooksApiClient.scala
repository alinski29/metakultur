package com.github.alinski.service

import com.github.alinski.io.{ApiClient, JsonEndpoint, SingleResourceEndpoint}
import com.github.alinski.model.Book
import sttp.client4.{Response, UriContext}
import sttp.model.Uri.PathSegment
import sttp.model.{Header, QueryParams}

object GoogleBooksApiClient extends ApiClient[Book]:
  val baseUri = uri"https://www.googleapis.com/books/v1"
  override val defaultHeaders = {
    sys.env.get("GOOGLE_BOOKS_TOKEN") match {
      case Some(token) if token.nonEmpty => super.defaultHeaders :+ Header("key", token)
      case _                             => super.defaultHeaders
    }
  }
