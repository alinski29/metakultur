package com.github.alinski.model

import java.time.LocalDate

enum ReadState:
  case Read, CurrentlyReading, Wishlist

case class Book(
    isbn13: Option[String],
    isbn10: Option[String],
    googleId: Option[String],
    goodreadsId: Option[String],
    title: String,
    subtitle: Option[String],
    author: String,
    additionalAuthors: Option[List[String]],
    description: Option[String],
    pageCount: Option[Int],
    publishedDate: Option[LocalDate],
    yearPublished: Option[String],
    publisher: Option[String],
    language: Option[String],
    categories: Option[List[String]],
    thumbnail: Option[String],
    posterUrl: Option[String],
    selfLink: Option[String],
    previewLink: Option[String],
    infoLink: Option[String],
    goodreadsRating: Option[Float] = None,
    // User preferences
    readStatus: Option[ReadState] = None,
    dateAdded: Option[LocalDate] = Some(LocalDate.now()),
    dateRead: Option[LocalDate] = None,
    dateRated: Option[LocalDate] = None,
    personalRating: Option[Int] = None,
)
