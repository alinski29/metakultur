package com.github.alinski.serde

import fastparse.*
import fastparse.NoWhitespace.*
import upickle.default.*

trait Parser[A]:
  def parse(input: String): Either[String, A]

object Parser:
  def apply(format: FileFormat): Parser[_] =
    format match
      case FileFormat.Json => JsonParser
      case FileFormat.Csv  => CSVParser

  def parse(input: String, format: FileFormat): Either[String, _] =
    apply(format).parse(input)

object CSVParser extends Parser[Seq[String]]:

  def quotedField[$: P]: P[String] =
    P("\"" ~~ CharsWhile(c => c != '\"').! ~~ "\"")

  def unquotedField[$: P]: P[String] =
    P(CharsWhile(c => c != ',' && c != '\n' && c != '\"').!)

  def field[$: P]: P[String] =
    P(quotedField | unquotedField)

  def row[$: P]: P[Seq[String]] =
    P(field.rep(sep = ","))

  def csv[$: P]: P[Seq[Seq[String]]] =
    P(row.rep(sep = "\n"))

  def parse(input: String): Either[String, Seq[String]] =
    fastparse.parse(input, row(_)) match
      case Parsed.Success(parsedRow, _) => Right(parsedRow)
      case f: Parsed.Failure            => Left(s"Parsing failed: ${f.trace().longMsg}")

// TODO: Needs to be referenced in the API
object JsonParser extends Parser[ujson.Value]:
  def parse(input: String): Either[String, ujson.Value] =
    try Right(ujson.read(input))
    catch
      case e: Exception =>
        Left(e.getMessage)

object ParsingUtils:

  def camelToSnake(s: String): CharSequence =
    s.replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
      .replaceAll("([a-z\\d])([A-Z])", "$1_$2")
      .toLowerCase

  def snakeToCamel(s: String): CharSequence =
    "_([a-z\\d])".r.replaceAllIn(
      s,
      m => m.group(1).toUpperCase()
    )

  def camelToKebab(str: String): String =
    str.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase

  object jsonExtensions:
    extension (obj: upickle.core.LinkedHashMap[String, ujson.Value])
      def hasField(field: String): Boolean =
        obj.keySet.contains(field)

      def hasFields(fields: Seq[String]): Boolean =
        fields.forall(obj.keySet.contains)

      def /(key: String): Option[ujson.Value] =
        obj.get(key) match
          case Some(v) => if v.isNull then None else Some(v)
          case None    => None

    extension (optObj: Option[ujson.Value])
      def /(key: String): Option[ujson.Value] = optObj.flatMap(_.obj.get(key))
