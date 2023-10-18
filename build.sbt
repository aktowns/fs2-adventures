name := "monix-adventures"

version := "0.1"

scalaVersion := "3.3.1"

val specs2Version = "5.3.2"
val fs2Version = "3.9.2"

libraryDependencies ++= Seq(
  "co.fs2" %% "fs2-core" % fs2Version,
  "org.typelevel" %% "cats-core" % "2.10.0",
  "org.typelevel" %% "cats-effect" % "3.5.2",
  "org.specs2" %% "specs2-core" % specs2Version % "test",
  "org.specs2" %% "specs2-matcher-extra" % specs2Version % "test"
)
