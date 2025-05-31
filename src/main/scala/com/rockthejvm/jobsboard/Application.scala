package com.rockthejvm.jobsboard

import cats.effect.{IO, IOApp}
import com.rockthejvm.jobsboard.config.EmberConfig
import com.rockthejvm.jobsboard.config.syntax.loadF
import com.rockthejvm.jobsboard.http.HttpApi
import org.http4s.Response
import org.http4s.ember.server.EmberServerBuilder
import pureconfig.ConfigSource

object Application extends IOApp.Simple {
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
