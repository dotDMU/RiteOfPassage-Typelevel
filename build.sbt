ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val rockthejvm    = "com.rockthejvm"
lazy val scala3Version = "3.3.6"

lazy val circeVersion  = "0.14.1"
lazy val catsEffectVersion          = "3.6.1"
lazy val http4sVersion              = "0.23.30"
lazy val doobieVersion              = "1.0.0-RC9"
lazy val pureConfigVersion          = "0.17.9"
lazy val log4catsVersion            = "2.7.0"
lazy val tsecVersion                = "0.5.0"
lazy val scalaTestVersion           = "3.2.19"
lazy val scalaTestCatsEffectVersion = "1.6.0"
lazy val testContainerVersion       = "1.21.0"
lazy val logbackVersion             = "1.5.18"
lazy val slf4jVersion               = "2.0.17"
lazy val javaMailVersion            = "1.6.2"

lazy val server = (project in file("."))
  .settings(
    name         := "typelevel-project",
    scalaVersion := scala3Version,
    organization := rockthejvm,
    libraryDependencies ++= Seq(
      "org.typelevel"         %% "cats-effect"         % catsEffectVersion,
      "org.http4s"            %% "http4s-dsl"          % http4sVersion,
      "org.http4s"            %% "http4s-ember-server" % http4sVersion,
      "org.http4s"            %% "http4s-circe"        % http4sVersion,
      "io.circe"              %% "circe-generic"       % circeVersion,
      "io.circe"              %% "circe-fs2"           % circeVersion,
      "org.tpolecat"          %% "doobie-core"         % doobieVersion,
      "org.tpolecat"          %% "doobie-hikari"       % doobieVersion,
      "org.tpolecat"          %% "doobie-postgres"     % doobieVersion,
      "org.tpolecat"          %% "doobie-scalatest"    % doobieVersion    % Test,
      "com.github.pureconfig" %% "pureconfig-core"     % pureConfigVersion,
      "org.typelevel"         %% "log4cats-slf4j"      % log4catsVersion,
      "org.slf4j"              % "slf4j-simple"        % slf4jVersion,
      "io.github.jmcardon"    %% "tsec-http4s"         % tsecVersion,
      "com.sun.mail"           % "javax.mail"          % javaMailVersion,
      "org.typelevel"         %% "log4cats-noop"       % log4catsVersion  % Test,
      "org.scalatest"         %% "scalatest"           % scalaTestVersion % Test,
      "org.typelevel"     %% "cats-effect-testing-scalatest" % scalaTestCatsEffectVersion % Test,
      "org.testcontainers" % "testcontainers"                % testContainerVersion       % Test,
      "org.testcontainers" % "postgresql"                    % testContainerVersion       % Test,
      "ch.qos.logback"     % "logback-classic"               % logbackVersion             % Test
    ),
  )