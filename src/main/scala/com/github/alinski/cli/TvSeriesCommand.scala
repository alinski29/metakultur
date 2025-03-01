package com.github.alinski.cli

import com.github.alinski.service.TvSeriesApiService

object TvSeriesCommand extends VisualMediaCommand:
  lazy val mediaService = TvSeriesApiService()
  override def name     = "tv"
