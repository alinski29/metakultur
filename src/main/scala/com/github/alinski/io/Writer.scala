package com.github.alinski.io

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
  lazy private val writer = new StreamingFileWriter {}

  def write(
      value: LazyList[String],
      path: os.Path,
      options: WriteOptions,
  ): Either[Throwable, File] =
    writer.write(value, path, options)
