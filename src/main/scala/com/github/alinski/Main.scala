package com.github.alinski

import caseapp.*
import com.github.alinski.ApiProvider.TheMovieDBProvider
import io.circe.generic.auto.*
import sttp.client4.*
import sttp.client4.circe.*
import sttp.client4.quickRequest.*

import scala.util.Failure
import scala.util.Success
import scala.util.Try

object MyApp extends CommandsEntryPoint:
  import CLI.*
  def progName = "metakultur"
  def commands = Seq(
    BookCommand,
    MovieCommand,
  )

object CLI:
  case class BookOptions(
      id: Option[String],
      goodreadsCSVFile: Option[String]
  )

  object BookCommand extends Command[BookOptions] {
    override def name = "book"
    def run(options: BookOptions, args: RemainingArgs): Unit = {
      scribe.info(s"Got book command. opts: $options: args: $args")
    }
  }

  case class MovieOptions(
      @Name("imdb-id")
      id: Option[String] = None,
      @Name("imdb-csv-file")
      imdbCSVFile: Option[String] = None
  )

  object MovieCommand extends Command[MovieOptions]:
    override def name = "movie"
    def run(opts: MovieOptions, args: RemainingArgs): Unit =
      if (opts.id.isEmpty && opts.imdbCSVFile.isEmpty)
      then throw IllegalArgumentException("No id or file provided, at least one is required")

      (opts.id, opts.imdbCSVFile) match
        case (Some(id), _) =>
          // scribe.info(s"Got movie command. id: $id")
          val apiKey = sys.env.get("THEMOVIEDB_TOKEN")
          val api    = TheMovieDBProvider(apiKey)
          api.getById(id) match
            case Success(value) =>
              scribe.info(s"Got movie command. id: $id, value: $value")
            case Failure(e) =>
              scribe.error(s"Got movie command. id: $id, error: $e")

        case (_, Some(file)) =>
          scribe.info(s"Got movie command. file: $file")
        case _ => ()
