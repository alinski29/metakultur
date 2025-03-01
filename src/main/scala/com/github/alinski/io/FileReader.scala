package com.github.alinski.io

import com.github.alinski.serde.CSVParser

import scala.util.{Failure, Success, Try, Using}

trait FileReader[T]:
  def read(path: os.Path): Either[Throwable, LazyList[T]]

trait CsvReader[T] extends FileReader[T]:

  def parse(line: Seq[String], header: Map[String, Int]): Either[Exception, T]

  def parseLine(line: String, columnMapping: Map[String, Int]): Either[Exception, T] =
    CSVParser.parse(line) match
      case Left(error) =>
        Left(new Exception(s"Error parsing line: $line, error: $error"))
      case Right(cols) =>
        parse(cols, columnMapping)

  def read(path: os.Path): Either[Throwable, LazyList[T]] =
    for
      header      <- readHeader(path)
      csvLines    <- readLines(path)
      parsedLines <- parseLines(csvLines, header)
    yield parsedLines

  def readHeader(path: os.Path): Either[Throwable, Map[String, Int]] =
    Using(scala.io.Source.fromFile(path.toIO))(_.getLines.take(1).toList.head) match
      case Failure(e)      => Left(e)
      case Success(header) => Right(header.split(",").zipWithIndex.toMap)

  def readLines(path: os.Path): Either[Throwable, Iterator[String]] =
    Try(scala.io.Source.fromFile(path.toIO).getLines.drop(1)).toEither

  private def parseLines(
      lines: Iterator[String],
      columnMapping: Map[String, Int]
  ): Either[Throwable, LazyList[T]] =
    lines.nextOption match
      case None       => Right(LazyList.empty)
      case Some(line) => parseLine(line, columnMapping).flatMap(m => parseLines(lines, columnMapping).map(m #:: _))
