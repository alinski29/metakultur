package com.github.alinski.service

import com.github.alinski.io.ApiClient
import com.github.alinski.model.Book
import sttp.client4.UriContext

object GoogleBooksApiClient extends ApiClient[Book]:
  val baseUri = uri"https://www.googleapis.com/books/v1"
