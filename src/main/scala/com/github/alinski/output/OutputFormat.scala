package com.github.alinski.output

// import io.circe.syntax._
// import io.circe.Encoder

// trait OutputFormat {
//   def serialize[T](data: T)(implicit encoder: Encoder[T]): String
// }

// class JsonFormat extends OutputFormat {
//   def serialize[T](data: T)(implicit encoder: Encoder[T]): String = data.asJson.noSpaces
// }

// class ConsoleFormat extends OutputFormat {
//   def serialize[T](data: T)(implicit encoder: Encoder[T]): String = data.toString
// }

// class YamlFormat extends OutputFormat {
//   def serialize[T](data: T)(implicit encoder: Encoder[T]): String = {
//     // Integrate a YAML library and implement serialization
//     // Example using circe-yaml (requires adding dependency)
//     ""
//   }
// }

// class CsvFormat extends OutputFormat {
//   def serialize[T](data: T)(implicit encoder: Encoder[T]): String = {
//     // Implement CSV serialization logic
//     // Example using a simple CSV formatter
//     import io.circe.JsonObject
//     data.asInstanceOf[Any] match {
//       case obj: JsonObject =>
//         obj.toMap.map { case (k, v) => s""""$k","${v.noSpaces.replace("\"", "\"\"")}"""" }.mkString("\n")
//       case _ => ""
//     }
//   }
// }
