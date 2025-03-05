package com.github.alinski.cli

import com.github.alinski.serde.FileFormat
import java.time.LocalDate

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
      .orElse(outputFile.map(FileFormat(_)))
      .getOrElse(FileFormat.Json)

  def parseDate(dateStr: String): Option[LocalDate] =
    try Some(LocalDate.parse(dateStr))
    catch case _: Exception => None
