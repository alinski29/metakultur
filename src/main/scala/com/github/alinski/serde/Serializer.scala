package com.github.alinski.serde

import com.github.alinski.model.{Book, VisualMediaType, VisualMedia, VisualMediaGenre, ViewState}
import upickle.default.*

import java.time.LocalDate
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.collection.immutable.ListMap
import com.github.alinski.model.ReadState
import com.github.alinski.serde.ParsingUtils.camelToKebab

trait Serializer[A]:
  def serialize(value: A, format: FileFormat): String

object Serializer:
  def apply[A](using s: Serializer[A]): Serializer[A] = s

  def serialize[A](
      value: A,
      format: FileFormat = FileFormat.Json,
  )(using serializer: Serializer[A]): String =
    serializer.serialize(value, format)

  given VisualMediaSerializer: Serializer[VisualMedia] with
    def serialize(value: VisualMedia, format: FileFormat): String =
      format match
        case FileFormat.Json     => Json.serialize(value)
        case FileFormat.Yaml     => Yaml.serialize(value)
        case FileFormat.Markdown => Markdown.serialize(value)
//         case FileFormat.MarkdownTable => MarkdownTable.serialize(value)
        case _ => Json.serialize(value)

  given BookSerializer: Serializer[Book] with
    def serialize(value: Book, format: FileFormat): String =
      format match
        case FileFormat.Json     => Json.serialize(value)
        case FileFormat.Yaml     => Yaml.serialize(value)
        case FileFormat.Markdown => Markdown.serialize(value)
        case _                   => Json.serialize(value)

  object Json:

    def serialize(movie: VisualMedia): String =
      MoviePickler.write(movie)

    def serialize(book: Book): String =
      BookPickler.write(book)

    trait JsonPickler extends upickle.AttributeTagged:
      import com.github.alinski.serde.ParsingUtils.{camelToSnake, snakeToCamel}

      override def objectAttributeKeyReadMap(s: CharSequence): CharSequence =
        snakeToCamel(s.toString)
      override def objectAttributeKeyWriteMap(s: CharSequence): CharSequence =
        camelToSnake(s.toString)

      override def objectTypeKeyReadMap(s: CharSequence): CharSequence =
        snakeToCamel(s.toString)
      override def objectTypeKeyWriteMap(s: CharSequence): CharSequence =
        camelToSnake(s.toString)

      given LocalDateReadWriter: ReadWriter[LocalDate] = readwriter[String].bimap[LocalDate](
        localDate => localDate.toString,
        localDateStr => LocalDate.parse(localDateStr)
      )

    object MoviePickler extends JsonPickler:
      override def objectAttributeKeyWriteMap(s: CharSequence): CharSequence =
        super.objectAttributeKeyWriteMap(s) match
          case "runtime" => "runtime_mins"
          case s         => s

      given durationReadWriter: ReadWriter[Duration] = readwriter[Int].bimap[Duration](
        duration => duration.toMinutes.toInt,
        minutes => Duration(minutes, TimeUnit.MINUTES)
      )

      given mediaTypeReadWriter: ReadWriter[VisualMediaType] = readwriter[String].bimap[VisualMediaType](
        mediaType => mediaType.toString.toLowerCase,
        mediaTypeStr =>
          // Title case
          VisualMediaType.valueOf(mediaTypeStr.zipWithIndex.map { case (c, i) =>
            if i == 0 then c.toUpper else c.toLower
          }.mkString)
      )

      given ViewStateReadWriter: ReadWriter[ViewState] = readwriter[String].bimap[ViewState](
        viewState => viewState.toString,
        viewStateStr => ViewState.valueOf(viewStateStr)
      )

      given MovieGenreReadWriter: ReadWriter[VisualMediaGenre] = readwriter[String].bimap[VisualMediaGenre](
        movieGenre => movieGenre.toString.toLowerCase,
        movieGenreStr => VisualMediaGenre.fromString(movieGenreStr).getOrElse(VisualMediaGenre.Unknown)
      )

      given movieRW: MoviePickler.ReadWriter[VisualMedia] = MoviePickler.macroRW

    object BookPickler extends JsonPickler:
      import com.github.alinski.serde.ParsingUtils.camelToKebab

      given mapReadWriter: ReadWriter[Map[String, String]] = readwriter[ujson.Obj].bimap[Map[String, String]](
        map => ujson.Obj.from(map.map { case (k, v) => (k, ujson.Str(v)) }),
        obj => obj.value.map { case (k, v) => (k, v.str) }.toMap
      )

      given ReadStateReadWriter: ReadWriter[ReadState] = readwriter[String].bimap[ReadState](
        readState => camelToKebab(readState.toString),
        readStateStr => ReadState.valueOf(readStateStr)
      )

      given bookRW: BookPickler.ReadWriter[Book] = BookPickler.macroRW

  object Yaml:
    import com.github.alinski.serde.ParsingUtils.{camelToKebab, camelToSnake}

    def serialize(movie: VisualMedia): String =
      val renames = Map(
        "runtime"     -> "runtime_minutes",
        "poster_url"  -> "cover",
        "view_status" -> "status",
      )
      valueToMap(movie, renames)
        .map { case (k, v) => s"${k}: $v" }
        .mkString("\n")

    def serialize(book: Book): String =
      valueToMap(book)
        .map { case (k, v) => s"${k}: $v" }
        .mkString("\n")

    def convertToString(value: Any): String = value match
      case s: String               => s"""\"$s\""""
      case d: Duration             => d.toMinutes.toString
      case ld: LocalDate           => ld.toString
      case g: VisualMediaGenre     => convertToString(camelToKebab(g.toString).toLowerCase)
      case m: VisualMediaType      => convertToString(camelToKebab(m.toString).toLowerCase)
      case inst: java.time.Instant => inst.toString
      case xs: List[_]             => xs.map(convertToString).mkString("[", ", ", "]")
      case m: Map[_, _]            => m.map { case (k, v) => s"$k: ${convertToString(v)}" }.mkString("\n")
      case None                    => ""
      case Some(v)                 => convertToString(v)
      case x                       => x.toString

    def valueToMap(
        value: Product,
        renames: Map[String, String] = Map.empty[String, String]
    ): ListMap[String, String] =
      val pairs = value.productElementNames.toList.zipWithIndex
        .map { case (name, idx) =>
          val finalName = camelToSnake(name).toString
          renames.getOrElse(finalName, finalName) -> convertToString(value.productElement(idx))
        }
        .filterNot { case (k, v) => v.isBlank }
      ListMap(pairs: _*)

  object Markdown:
    def serialize(movie: VisualMedia): String =
      val renames = Map(
        "runtime"    -> "runtime_minutes",
        "poster_url" -> "cover"
      )
      serialize(Yaml.valueToMap(movie.copy(description = None), renames), movie.description, movie.posterUrl)

    def serialize(book: Book): String =
      val pairs = Yaml.valueToMap(book.copy(description = None)).map { case (k, v) =>
        val vv = k match
          case k if k == "read_status" => s"${camelToKebab(v)}"
          case _                       => v
        k -> vv
      }
      serialize(pairs, description = book.description, posterUrl = book.posterUrl)

    def serialize(data: ListMap[String, String], description: Option[String], posterUrl: Option[String]) =
      val yaml        = data.map { case (k, v) => s"${k}: ${v}" }.mkString("\n")
      val renderCover = posterUrl.map(p => s"![|300](${p})").getOrElse("")

      List(
        "---",
        yaml,
        "---",
        "\n",
        renderCover,
        "\n",
        "## Description",
        description.getOrElse("")
      ).mkString("\n")

  object MarkdownTable:
    def serialize(rows: Seq[Seq[String]], headers: Seq[String]): String =
      val columnWidths = (headers +: rows).transpose.map(_.map(_.length).max)

      val formatRow = (row: Seq[String]) =>
        row.zip(columnWidths).map { case (cell, width) => cell.padTo(width, ' ') }.mkString("| ", " | ", " |")

      val headerRow    = formatRow(headers)
      val separatorRow = columnWidths.map("-" * _).mkString("|-", "-|-", "-|")
      val dataRows     = rows.map(formatRow)

      (headerRow +: separatorRow +: dataRows).mkString("\n")
