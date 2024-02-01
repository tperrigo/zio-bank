scalaVersion := "2.13.12"
name := "zio-bank"
version := "0.0.1"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % "2.0.21",
  "dev.zio" %% "zio-json" % "latest.release",
  "dev.zio" %% "zio-http" % "3.0.0-RC2",
  "dev.zio" %% "zio-test" % "2.0.21" % Test,
  "dev.zio" %% "zio-test-sbt" % "2.0.21" % Test
)

