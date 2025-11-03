package com.rockthejvm.jobsboard

import cats.effect.{IO, IOApp}
import com.rockthejvm.jobsboard.config.AppConfig
import com.rockthejvm.jobsboard.config.syntax.loadF
import com.rockthejvm.jobsboard.modules.*
import org.http4s.Response
import org.http4s.ember.server.EmberServerBuilder
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource

object Application extends IOApp.Simple {
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] =
    ConfigSource.default.loadF[IO, AppConfig].flatMap {
      case AppConfig(postgresConfig, emberConfig) =>
        val appResource = for {
          xa      <- Database.makePostgresResource[IO](postgresConfig)
          core    <- Core[IO](xa)
          httpApi <- HttpApi[IO](core)
          server  <- EmberServerBuilder
            .default[IO]
            .withHost(emberConfig.host)
            .withPort(emberConfig.port)
            .withHttpApp(httpApi.endpoints.orNotFound)
            .build
        } yield server

        appResource.use(_ => IO.println("Server ready!") *> IO.never)
    }
}
