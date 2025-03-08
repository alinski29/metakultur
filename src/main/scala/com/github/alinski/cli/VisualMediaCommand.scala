package com.github.alinski.cli

import caseapp.*
import com.github.alinski.cli.Helpers.*
import com.github.alinski.io.{FileWriter, WriteOptions}
import com.github.alinski.model.{ViewState, VisualMedia}
import com.github.alinski.serde.Serializer.MarkdownTable
import com.github.alinski.serde.Serializer
import com.github.alinski.service.{ImdbCsvReaderService, VisualMediaApiService}

import scala.annotation.tailrec
import java.time.LocalDate

trait VisualMediaCommand extends Command[VisualMediaOptions]:
  def name: String
  def mediaService: VisualMediaApiService

  override def run(options: VisualMediaOptions, args: RemainingArgs): Unit =
    val maybeMedia = (options.imdbId, options.tmdbId, options.imdbCSVFile) match
      case (Some(imdbId), _, _) =>
        for
          mediaImdb <- mediaService.getByImdbId(imdbId)
          mediaTmdb <- mediaService.getById(mediaImdb.imdbId)
        yield LazyList(mediaTmdb)
      case (_, Some(tmdbId), _) =>
        mediaService.getById(tmdbId).map(LazyList(_))
      case (_, _, Some(filePath)) =>
        val result = ImdbCsvReaderService.read(
          os.Path(filePath),
          options.shared.from.flatMap(parseDate),
          options.shared.to.flatMap(parseDate)
        )
        if options.shared.enrich
        then result.map(enrichMedia)
        else result
      case (None, None, None) if args.all.nonEmpty =>
        userPromptInteraction(mediaService, args.all.mkString(" "), options.shared.limit, options.shared.rate)
      case _ =>
        Left(
          IllegalArgumentException("No imdb-id, tmdb-id, file or search query provided. At least one is required")
        )

    // Apply user preferences if rate option is enabled and we have a direct ID lookup
    val moviesWithPreferences =
      if options.shared.rate && options.imdbId.isDefined && maybeMedia.isRight
      then maybeMedia.map(_.map(collectUserPreferences))
      else maybeMedia

    moviesWithPreferences match
      case Left(err) if err.getMessage.contains("No items found") || err.getMessage.contains("Empty items") =>
        println(s"No movie found for id: ${options.imdbId.getOrElse("")}")
        sys.exit(0)
      case Left(err) =>
        throw err
      case Right(movies) if movies.isEmpty && options.imdbId.nonEmpty =>
        println(s"No movie found for id: ${options.imdbId.getOrElse("")}")
        sys.exit(0)
      case Right(movies) => ()

    val writeOpts = WriteOptions.default
    val maybePath = options.shared.output.map(os.Path(_))
    maybePath.foreach { path => setupDirectoriesIfNeeded(path, writeOpts) }

    val format = resolveOutputFormat(options.shared.outputFormat, options.shared.output)
    val movies = moviesWithPreferences.getOrElse(LazyList.empty[VisualMedia])

    maybePath match
      case None =>
        movies.foreach(movie => println(Serializer.serialize(movie, format)))
      case Some(path) if path.ext.isBlank =>
        movies.foreach { movie =>
          val filePath = path / Helpers.createValidFileName(movie.title, format.toString)
          val output   = Serializer.serialize(movie, format)
          FileWriter.write(LazyList(output), filePath, writeOpts) match
            case Right(file) =>
              scribe.info(s"Written to $file")
            case Left(err) =>
              scribe.error(s"Failed to write to $filePath, error: ${err.getMessage}")
              throw err
        }
      case Some(path) =>
        val output = movies.map(Serializer.serialize(_, format))
        FileWriter.write(output, path, writeOpts) match
          case Right(file) =>
            scribe.info(s"Written to $file")
          case Left(err) =>
            scribe.error(s"Failed to write to file $path, error: ${err.getMessage}")
            throw err

  @tailrec
  private def userPromptInteraction(
      service: VisualMediaApiService,
      query: String,
      limit: Int,
      rate: Boolean = false
  ): Either[Exception, LazyList[VisualMedia]] =
    service.search(query, limit = limit) match
      case Left(err) => Left(err)
      case Right(movies) if movies.nonEmpty =>
        val selection = promptUserToSelectMovie(movies)
        val media     = selection.flatMap(movie => service.getById(movie.head.tmdbId.get))
        // Apply user preferences if rate is enabled
        if rate && media.isRight then media.map(m => LazyList(collectUserPreferences(m)))
        else media.map(LazyList(_))
      case Right(movies) =>
        println(s"No results found for your search query: '$query'. Enter a new query or press Ctrl+C to exit")
        val newQuery = scala.io.StdIn.readLine()
        userPromptInteraction(service, newQuery, limit)

  private def promptUserToSelectMovie(movies: LazyList[VisualMedia]): Either[Exception, LazyList[VisualMedia]] =
    if movies.isEmpty then return Left(Exception("No movies found"))
    println("Select a movie by its index: \n")

    val maxLength = 60
    val headers   = Seq("Index", "Title", "Year", "Lang", "Genres")
    val rows = movies.zipWithIndex.map { case (movie, index) =>
      val genres   = movie.genres.getOrElse(List()).mkString(", ")
      val language = movie.originalLanguage.getOrElse("")
      Seq(
        (index + 1).toString,
        movie.title.take(maxLength),
        movie.year.getOrElse("N/A"),
        language,
        genres.take(maxLength)
      )
    }

    val markdownTable = MarkdownTable.serialize(rows, headers)
    println(markdownTable)

    try
      val selectedIndex = scala.io.StdIn.readInt() - 1
      if selectedIndex < 0 || selectedIndex >= movies.size then
        println("Invalid selection, please try again")
        Left(Exception("Invalid selection"))
      else Right(LazyList(movies(selectedIndex)))
    catch
      case _: Exception =>
        println("Invalid input, please enter a number")
        Left(Exception("Invalid input"))

  private def collectUserPreferences(media: VisualMedia): VisualMedia =
    val renderName = media.title + media.year.fold("")(" (" + _ + ")")

    println(s"What's the view status of $renderName ?")
    println("1. I've seen it already")
    println("2. I'd like to see it (wishlist)")

    val statusChoice =
      try scala.io.StdIn.readInt()
      catch
        case _: Exception =>
          println("Invalid input, defaulting to wishlist")
          2

    val viewStatus = statusChoice match
      case 1 => ViewState.Viewed
      case 2 => ViewState.Whishlist
      case _ =>
        println("Invalid choice, defaulting to wishlist")
        ViewState.Whishlist

    val (dateViewed, personalRating) = if viewStatus == ViewState.Viewed then
      println(s"When have you seen $renderName ? Enter a date in format yyyy-MM-dd or leave blank.")
      val dateStr = scala.io.StdIn.readLine().trim
      val date =
        if dateStr.isEmpty then None
        else
          try Some(LocalDate.parse(dateStr))
          catch
            case _: Exception =>
              println("Invalid date format, using today's date")
              Some(LocalDate.now())

      println(s"Rate $renderName on a scale from 1 to 10")
      val rating =
        try
          val r = scala.io.StdIn.readInt()
          if r < 1 || r > 10 then
            println(s"Invalid rating: $r not between 1 and 10, no rating set")
            None
          else Some(r)
        catch
          case err: Exception =>
            println(s"Some error occured, can't set rating: ${err.getMessage}")
            None

      (date, rating)
    else (None, None)

    media.copy(
      viewStatus = Some(viewStatus),
      dateViewed = dateViewed,
      dateRated = if personalRating.isDefined then Some(LocalDate.now()) else None,
      personalRating = personalRating
    )

  private def enrichMedia(records: LazyList[VisualMedia]): LazyList[VisualMedia] =
    records.map { record =>
      record.imdbId match
        case "" => record
        case imdbId =>
          mediaService.getByImdbId(imdbId) match
            case Right(recordEnriched) =>
              record.copy(
                tmdbId = recordEnriched.tmdbId,
                description = recordEnriched.description,
                posterUrl = recordEnriched.posterUrl,
                originCountry = recordEnriched.originCountry,
                originalLanguage = recordEnriched.originalLanguage,
                genres = record.genres.orElse(recordEnriched.genres),
                runtime = record.runtime.orElse(recordEnriched.runtime),
                tmdbRating = recordEnriched.tmdbRating
              )
            case Left(err) =>
              scribe.warn(s"Failed to fetch extra information for movie: $imdbId, title: ${record.title}")
              record
    }
