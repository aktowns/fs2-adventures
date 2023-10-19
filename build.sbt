name := "fs2-adventures"

version := "0.1"

scalaVersion := "3.3.1"

val specs2Version = "5.3.2"
val fs2Version    = "3.9.2"

libraryDependencies ++= Seq(
  "co.fs2"        %% "fs2-core"            % fs2Version,
  "org.typelevel" %% "cats-core"           % "2.10.0",
  "org.typelevel" %% "cats-effect"         % "3.5.2",
  "org.scalameta" %% "munit"               % "0.7.29"   % Test,
  "org.typelevel" %% "munit-cats-effect"   % "2.0.0-M3" % Test,
  "org.typelevel" %% "cats-effect-testkit" % "3.5.2"    % Test
)
