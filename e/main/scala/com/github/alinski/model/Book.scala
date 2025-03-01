package com.github.alinski.model

import java.time.LocalDate

case class Book(
    googleId: String,
    title: String,
    subtitle: Option[String],
    authors: List[String],
    description: Option[String],
    pageCount: Option[Int],
    publishedDate: Option[LocalDate],
    publisher: Option[String],
    isbn10: Option[String],
    isbn13: Option[String],
    language: Option[String],
    categories: Option[List[String]],
    imageLinks: Option[Map[String, String]],
    selfLink: Option[String],
    previewLink: Option[String],
    infoLink: Option[String]
)
