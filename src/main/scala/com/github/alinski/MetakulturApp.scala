package com.github.alinski

import caseapp.*
import com.github.alinski.cli.{BookCommand, MovieCommand, TvSeriesCommand}

object MetakulturApp extends CommandsEntryPoint:
  def progName = "metakultur"
  def commands = Seq(
    BookCommand,
    MovieCommand,
    TvSeriesCommand
  )
