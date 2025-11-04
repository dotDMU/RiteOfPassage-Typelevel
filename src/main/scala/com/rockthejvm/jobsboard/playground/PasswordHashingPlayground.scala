package com.rockthejvm.jobsboard.playground

import cats.effect.*
import cats.implicits.*
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

import scala.language.postfixOps

object PasswordHashingPlayground extends IOApp.Simple {

  override def run: IO[Unit] = {
    BCrypt.hashpw[IO]("scalarocks").flatMap(IO.println) *>
      BCrypt.checkpwBool[IO](
        "scalarocks",
        PasswordHash[BCrypt]("$2a$10$RYrElABPBkhM79b3GLXVvel/xE1tfSKIIhYeqXJ6suSBhudzw6Fam")
      ).flatMap(IO.println)

      //IO.pure(())
  }
}
