package com.github.alinski.serde

enum FileFormat:
  case Json, Yaml, Csv, MarkdownTable

object FileFormat:
  val default: FileFormat = Json

  def apply(s: String): FileFormat =
    s match
      case "json"  => Json
      case "yaml"  => Yaml
      case "csv"   => Csv
      case "table" => MarkdownTable
      case _       => default

  def fromFile(file: java.io.File): FileFormat =
    file.getName.split('.').lastOption match
      case Some(v) => FileFormat(v)
      case None =>
        scribe.warn(s"No known file extension found, defaulting $default")
        default
