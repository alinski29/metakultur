package com.github.alinski.cli

import com.github.alinski.serde.FileFormat
import java.time.LocalDate
import com.github.alinski.io.WriteOptions

object Helpers:
  def createValidFileName(title: String, extension: String): String =
    val sanitizedTitle = title
      .replaceAll("[^a-zA-Z0-9\\s]", "") // remove invalid characters, but keep spaces
      .trim
      .take(255 - extension.length - 1)

    s"$sanitizedTitle.$extension"

  def resolveOutputFormat(outputFormat: Option[String], outputFile: Option[String]): FileFormat =
    outputFormat
      .map(FileFormat(_))
      .orElse(outputFile.map(filePath => FileFormat(os.Path(filePath).ext)))
      .getOrElse(FileFormat.Json)

  def parseDate(dateStr: String): Option[LocalDate] =
    try Some(LocalDate.parse(dateStr))
    catch case _: Exception => None

  def setupDirectoriesIfNeeded(path: os.Path, writeOpts: WriteOptions = WriteOptions.default) =
    val destDir = if path.ext.nonEmpty then os.Path(path.toIO.getParent) else path
    if !destDir.toIO.exists && writeOpts.makeDirs
    then os.makeDir.all(destDir)
    else if !destDir.toIO.exists && !writeOpts.makeDirs
    then throw IllegalArgumentException(s"Path $path does not exist and makeDirs is set to false")
