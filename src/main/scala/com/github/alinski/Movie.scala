package com.github.alinski

import scala.concurrent.duration.Duration
import java.time.{Instant, LocalDate, ZoneId}

enum ViewState:
  case Viewed, Whishlist

enum MediaType:
  case Movie, TVSeries, TVMiniSeries

trait MovieMetadata {
  def imdbId: String
  def title: String
  def originalTitle: String
  def `type`: MediaType
  def dateReleased: LocalDate
  def year: String
  def directors: List[String]
  def genres: List[String]
  def runtime: Option[Duration]
  def description: Option[String]
  def posterUrl: Option[String]
}

trait MoviePreferences {
  def viewStatus: ViewState
  def dateRated: LocalDate
  def personalRating: Option[Int]
}

case class Movie(
    imdbId: String,
    title: String,
    originalTitle: String,
    `type`: MediaType,
    imdbRating: Double,
    dateReleased: LocalDate,
    year: String,
    directors: List[String],
    genres: List[String],
    runtime: Option[Duration] = None,
    description: Option[String] = None,
    posterUrl: Option[String] = None,
) extends MovieMetadata

case class MovieWithPreference(
    imdbId: String,
    title: String,
    originalTitle: String,
    `type`: MediaType,
    imdbRating: Double,
    dateReleased: LocalDate,
    year: String,
    directors: List[String],
    genres: List[String],
    runtime: Option[Duration] = None,
    description: Option[String] = None,
    posterUrl: Option[String] = None,
    viewStatus: ViewState,
    dateRated: LocalDate,
    personalRating: Option[Int]
) extends MovieMetadata with MoviePreferences
