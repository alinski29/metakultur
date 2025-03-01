# Metakultur

Metakultur is a CLI application written in Scala 3 native that fetches metadata about cultural events from web APIs. It currently supports books and movies, with plans to expand to concerts, craft beer, theater plays, and more.

## Description

Metakultur is designed to be an extensible and flexible tool for fetching and processing metadata about various cultural resources. The application supports multiple output formats and can read from different data sources, making it a versatile solution for managing cultural event data.

## Main Features

- Fetch resource by ID from API
- Search resource with user query, give top n results to the user to choose 1, then fetch the resource by ID
- For books: read from an IMDB CSV file export, parse and convert it to the application format
- For movies: read from a Goodreads CSV file export, parse the content and convert it to the application format
- Support multiple output formats: console (default), JSON, YAML, and CSV, with an option like `-o` or `--output`

## Usage

```bash
metakultur movie --id <id>
metakultur movie <search query>
metakultur movie --imdb-csv-file <path-to-csv-file>

metakultur tv --id <id>
metakultur tv <search query>
metakultur tv --imdb-csv-file <path-to-csv-file>

metakultur book --id <id>
metakultur book search <search query>
metakultur book --goodreads-csv-file <path-to-csv-file>
```