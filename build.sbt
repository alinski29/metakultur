scalaVersion := "3.3.5" // A Long Term Support version.

organization := "com.github.alinski"
name         := "metakultur"
version      := "0.1.0"
logLevel     := Level.Info

lazy val root = (project in file("."))
  .enablePlugins(ScalaNativePlugin)
  .settings(
    Compile / mainClass := Some(s"com.github.alinski.MetakulturApp"),
    scalafmtOnCompile := true,
    scalacOptions ++= Seq("-explain", "-Wunused:imports", "-Wunused:locals", "-Wunused:params", "-Wunused:linted"),
    libraryDependencies ++= Seq(
      "com.github.alexarchambault"    %% "case-app_native0.5"        % "2.1.0-M30",
      "com.lihaoyi"                   %% "os-lib_native0.5"          % "0.11.4",
      "com.lihaoyi"                   %% "fastparse_native0.5"       % "3.1.1",
      "com.lihaoyi"                   %% "upickle_native0.5"         % "4.1.0",
      "com.outr"                      %% "scribe_native0.5"          % "3.16.0",
      "io.github.cquiroz"             %% "scala-java-time_native0.5" % "2.6.0",
      "com.softwaremill.sttp.client4" %% "core_native0.5"            % "4.0.0-RC1",
      "org.scalameta"                 %% "munit_native0.5"           % "1.1.0" % Test
    )
  )

// import to add Scala Native options
import scala.scalanative.build._

// defaults set with common options shown
nativeConfig ~= { c =>
  c.withLTO(LTO.none)           // thin
    .withMode(Mode.releaseSize) // releaseFast
    .withGC(GC.immix)           // commix
}
