package com.rockthejvm.jobsboard

import cats.effect.{IO, IOApp}
import com.rockthejvm.jobsboard.config.EmberConfig
import com.rockthejvm.jobsboard.config.syntax.loadF
import com.rockthejvm.jobsboard.http.HttpApi
import org.http4s.Response
import org.http4s.ember.server.EmberServerBuilder
import pureconfig.ConfigSource
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger


object Application extends IOApp.Simple {
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  
  override def run: IO[Unit] = ConfigSource.default.at("ember-config").loadF[IO, EmberConfig].flatMap{ config =>
        EmberServerBuilder
          .default[IO]
          .withHost(config.host)
          .withPort(config.port)
          .withHttpApp(HttpApi[IO].endpoints.orNotFound)
          .build
          .use(_ => IO.println("Server ready!") *> IO.never)
    }
}
