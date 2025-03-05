package com.github.alinski.io

import com.github.alinski.serde.FileFormat

import java.io.File
import java.nio.file.FileAlreadyExistsException

case class WriteOptions(overwrite: Boolean, makeDirs: Boolean)
object WriteOptions:
  val default: WriteOptions = WriteOptions(overwrite = false, makeDirs = true)

trait FileWriter[T]:
  def defaultOptions: WriteOptions = WriteOptions.default
  def write(value: T, path: os.Path, opts: WriteOptions = defaultOptions): Either[Throwable, File]

trait StreamingFileWriter extends FileWriter[LazyList[String]]:
  def write(value: LazyList[String], path: os.Path, opts: WriteOptions = defaultOptions): Either[Throwable, File] =
    val file = path.toIO
    if file.exists() && !opts.overwrite then Left(new FileAlreadyExistsException(path.toString))
    else
      val writer = new java.io.PrintWriter(file)
      try
        value.foreach(writer.write)
        Right(file)
      catch case e: Throwable => Left(e)
      finally writer.close()

object FileWriter:
//  def apply[T](using s: FileWriter[T]): FileWriter[T] = s

  def write(
      value: LazyList[String],
      path: os.Path,
      options: WriteOptions,
      // format: Option[FileFormat] = None
  ): Either[Throwable, File] =
    new StreamingFileWriter {}.write(value, path, options)
  // // format match
  //   case Some(FileFormat.Json) => new StreamingFileWriter {}.write(value, path, options)
  //   case Some(FileFormat.Csv)  => new StreamingFileWriter {}.write(value, path, options)
  //   case Some(FileFormat.Markdown)  => new StreamingFileWriter {}.write(value, path, options)
