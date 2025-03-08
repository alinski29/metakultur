package com.github.alinski.model

import scala.concurrent.duration.Duration
import java.time.LocalDate

enum ViewState:
  case Viewed, Whishlist

enum VisualMediaType:
  case Movie, TvSeries, Unknown

enum VisualMediaGenre(val id: Int):
  case Action         extends VisualMediaGenre(28)
  case Adventure      extends VisualMediaGenre(12)
  case Animation      extends VisualMediaGenre(16)
  case Biography      extends VisualMediaGenre(2000) // Placeholder ID
  case Comedy         extends VisualMediaGenre(35)
  case Crime          extends VisualMediaGenre(80)
  case Documentary    extends VisualMediaGenre(99)
  case Drama          extends VisualMediaGenre(18)
  case Family         extends VisualMediaGenre(10751)
  case Fantasy        extends VisualMediaGenre(14)
  case History        extends VisualMediaGenre(36)
  case Horror         extends VisualMediaGenre(27)
  case Music          extends VisualMediaGenre(10402)
  case Mystery        extends VisualMediaGenre(9648)
  case Romance        extends VisualMediaGenre(10749)
  case ScienceFiction extends VisualMediaGenre(878)
  case Short          extends VisualMediaGenre(2002) // Placeholder ID
  case Sport          extends VisualMediaGenre(2003) // Placeholder ID
  case TVMovie        extends VisualMediaGenre(10770)
  case Thriller       extends VisualMediaGenre(53)
  case War            extends VisualMediaGenre(10752)
  case Western        extends VisualMediaGenre(37)
  case Unknown        extends VisualMediaGenre(0)

object VisualMediaGenre:
  def fromId(id: Int): Option[VisualMediaGenre] =
    VisualMediaGenre.values.find(_.id == id)

  def fromString(name: String): Option[VisualMediaGenre] =
    name match
      case "Sci-Fi" | "sci-fi"   => Some(ScienceFiction)
      case "Musical" | "musical" => Some(Music)
      case other => VisualMediaGenre.values.find(x => x.toString.toLowerCase == other.toString.toLowerCase)

case class VisualMedia(
    tmdbId: Option[String],
    imdbId: String,
    title: String,
    originalTitle: Option[String],
    originalLanguage: Option[String],
    `type`: VisualMediaType,
    imdbRating: Option[Float] = None,
    tmdbRating: Option[Float] = None,
    dateReleased: Option[LocalDate],
    year: Option[String],
    directors: List[String],
    genres: Option[List[VisualMediaGenre]],
    runtime: Option[Duration] = None,
    description: Option[String] = None,
    posterUrl: Option[String] = None,
    originCountry: Option[String] = None,
    // User preferences. Maybe think of separating these into a different model
    dateAdded: Option[LocalDate] = Some(LocalDate.now()),
    viewStatus: Option[ViewState] = None,
    dateViewed: Option[LocalDate] = None,
    dateRated: Option[LocalDate] = None,
    personalRating: Option[Int] = None,
)
