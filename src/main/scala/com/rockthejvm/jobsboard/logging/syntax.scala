package com.rockthejvm.jobsboard.logging

import cats.MonadError
import org.typelevel.log4cats.Logger
import cats.implicits.*

object syntax {
  extension [F[_], E, A](fa: F[A])(using moError: MonadError[F, E], logger: Logger[F]) {
    def log(success: A => String, error: E => String): F[A] = fa.attemptTap {
      case Left(err) => logger.error(error(err))
      case Right(a) => logger.info(success(a))
    }

    def logError(error: E => String): F[A] = fa.attemptTap {
      case Left(err) => logger.error(error(err))
      case Right(_) => ().pure[F]
    }
  }
}

//Ja, genau! Die definierte extension-Methode logError (und auch log) in syntax ermöglicht es dir, an jeden Effekt F[A],
// bei dem ein MonadError[F, E] und ein Logger[F] im Gültigkeitsbereich(Scope) sind (via using),
// Logging-Funktionalität anzuhängen – ohne explizit Logger oder attempt aufzurufen.
//Zusammengefasst:
//
//Ja, du kannst an jeden Effekt F[A], der in einem Kontext mit MonadError[F, E] und Logger[F] steht, ein Logging anhängen – besonders für Fehlerfälle mit logError.
//Was passiert genau bei logError?
//
//def logError(error: E => String): F[A] = fa.attemptTap {
//  case Left(err) => logger.error(error(err))
//  case Right(_) => ().pure[F]
//}
//
//    fa.attemptTap: wandelt fa in ein F[Either[E, A]] um, lässt aber das ursprüngliche fa durchlaufen (ähnlich wie ein "peek" auf das Ergebnis).
//
//    Falls fa fehlschlägt, wird logger.error aufgerufen.
//
//    Falls fa erfolgreich ist, passiert nichts (nur pure(())).
//
//Beispielanwendung:
//
//jobInfo <- request.as[JobInfo].logError(e => s"Parsing payload failed: $e")
//
//    request.as[JobInfo] ist z. B. ein F[JobInfo] (vermutlich IO[JobInfo])
//
//    Falls das Parsen fehlschlägt (z. B. JSON-Parsing), wird ein Fehler geloggt
//
//    Falls es klappt, geht's weiter, ohne Logging
