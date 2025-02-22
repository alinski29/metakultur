package com.github.alinski

import munit.FunSuite
import scala.util.Try
import io.circe.parser.*
import io.circe.Json

import ApiProvider.TheMovieDBProvider
import scala.util.Failure
import scala.util.Success

class APIProviderSpec extends FunSuite:
  import APIProviderSpec.*

  val apiMock = new TheMovieDBProvider():
    override def getById(id: String): Try[Json] = parse(responseTVSeries) match
      case Left(e)  => Failure(e)
      case Right(v) => Success(v)

  test("API Provider") {
    assertEquals(1, 1)
    val result = apiMock.getById("6132")
    assert(result.isSuccess)
  }

object APIProviderSpec:
  private val responseTVSeries: String = """
  {
    "movie_results": [],
    "person_results": [],
    "tv_results": [
      {
        "backdrop_path": "/A6gI6Z4Bsw2rWMqJP78SCahEZ8q.jpg",
        "id": 6132,
        "name": "The Power of Nightmares",
        "original_name": "The Power of Nightmares",
        "overview": "Examines how politicians have used our fears to increase their power and control over society.",
        "poster_path": "/tCzglFkgrkX5t7nLpvIgOJ3KCO7.jpg",
        "media_type": "tv",
        "adult": false,
        "original_language": "en",
        "genre_ids": [
          99
        ],
        "popularity": 7.881,
        "first_air_date": "2004-10-20",
        "vote_average": 8.1,
        "vote_count": 69,
        "origin_country": [
          "GB"
        ]
      }
    ],
    "tv_episode_results": [],
    "tv_season_results": []
  }
  """
