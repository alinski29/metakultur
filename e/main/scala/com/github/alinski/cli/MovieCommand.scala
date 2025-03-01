package com.github.alinski.cli

import caseapp.*
import com.github.alinski.service.MovieApiService
import sttp.client4.quickRequest.*

object MovieCommand extends VisualMediaCommand:
  lazy val mediaService = MovieApiService()
  override def name     = "movie"
