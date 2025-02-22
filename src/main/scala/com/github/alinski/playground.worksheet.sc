import io.circe.Json
import io.circe.generic.auto.*
import io.circe.parser.*
import sttp.client4.*
import sttp.client4.circe.*
import sttp.client4.curl.CurlBackend
import sttp.client4.quick.basicRequest
import sttp.client4.quickRequest.*
import sttp.model.{Header, Uri, QueryParams}

import scala.util.Failure
import scala.util.Success
import scala.util.Try

val hello = "Hello, world!"
println(hello)