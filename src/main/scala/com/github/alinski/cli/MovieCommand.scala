package com.github.alinski.cli

import caseapp.*
import com.github.alinski.service.MovieApiService

object MovieCommand extends VisualMediaCommand:
  lazy val mediaService = MovieApiService()
  override def name     = "movie"
