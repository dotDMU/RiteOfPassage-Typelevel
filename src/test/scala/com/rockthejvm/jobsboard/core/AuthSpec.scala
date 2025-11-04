package com.rockthejvm.jobsboard.core

import cats.data.OptionT
import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import com.rockthejvm.jobsboard.domain.auth.NewPasswordInfo
import com.rockthejvm.jobsboard.domain.security.Authenticator
import com.rockthejvm.jobsboard.domain.user.{NewUserInfo, Role, User}
import com.rockthejvm.jobsboard.fixtures.UsersFixture
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import tsec.authentication.{IdentityStore, JWTAuthenticator}
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

import scala.concurrent.duration.DurationInt

class AuthSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with DoobieSpec
    with UsersFixture {
  override val initScript: String = "sql/users.sql"

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  private val mockedUsers: Users[IO] = new Users[IO] {
    override def find(email: String): IO[Option[User]] = {
      if (email == danielEmail) IO.pure(Some(Daniel))
      else IO.pure(None)
    }
    override def create(user: User): IO[String]        = IO.pure(user.email)
    override def update(user: User): IO[Option[User]]  = IO.pure(Some(user))
    override def delete(email: String): IO[Boolean]    = IO.pure(true)
  }

  val mockedAuthenticator: Authenticator[IO] = {
    // key for hashing
    val key = HMACSHA256.unsafeGenerateKey

    // identity store
    val idStore: IdentityStore[IO, String, User] = (email: String) => {
      if (email == danielEmail) OptionT.pure(Daniel)
      else if (email == riccardoEmail) OptionT.pure(Riccardo)
      else OptionT.none[IO, User]
    }

    // Jwt Authenticator
    JWTAuthenticator.unbacked.inBearerToken(
      1.day,
      None,
      idStore,
      key
    )
  }

  "Auth 'algebra" - {
    "login should return none if the users doesnt exist" in {
      val program = for {
        auth       <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
        maybeToken <- auth.login("user@rockthejvm.com", "password")
      } yield maybeToken

      IO(true).asserting(_ shouldBe true)
    }
  }

  "login should return a token if a user exists and the password is correct" in {
    val program = for {
      auth       <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
      maybeToken <- auth.login(danielEmail, "rockthejvm")
    } yield maybeToken

    program.asserting(_ shouldBe defined)
  }

  "signing up should not create a user with an existing email" in {
    val program = for {
      auth      <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
      maybeUser <- auth.signUp(
        NewUserInfo(
          danielEmail,
          "somePassword",
          Some("Daniel"),
          Some("Whatever"),
          Some("Other company")
        )
      )
    } yield maybeUser

    program.asserting(_ shouldBe defined)
  }

  "signing up should create a new user" in {
    val program = for {
      auth      <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
      maybeUser <- auth.signUp(
        NewUserInfo(
          "bob@rockthejvm",
          "somePassword",
          Some("Bob"),
          Some("Jones"),
          Some("Company")
        )
      )
    } yield maybeUser

    program.asserting {
      case Some(user) =>
        user.email shouldBe "bob@rockthejvm"
        user.firstName shouldBe Some("Bob")
        user.lastName shouldBe Some("Jones")
        user.company shouldBe Some("Company")
        user.role shouldBe Role.RECRUITER
      case _          => fail()
    }
  }

  "change password should return none if the user doesnt exist" in {
    val program: IO[Either[MyError, User]] = for {
      auth   <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
      result <- auth.changePassword("alice@rockthejvm.com", NewPasswordInfo("oldPw", "newPw")).value
    } yield result

    program.asserting(_ shouldBe Left(MyError.StandardError))
  }

  "change password should correctly change password" in {
    val program: IO[Boolean] = for {
      auth   <- LiveAuth[IO](mockedUsers, mockedAuthenticator)
      result <- auth.changePassword(danielEmail, NewPasswordInfo("rockthejvm", "scalarocks")).value
      passwdIsCorrect <- result match {
        case Right(user) =>
          BCrypt.checkpwBool[IO](
            "scalarocks",
            PasswordHash[BCrypt](user.hashedPassword)
          )
        case _           =>
          IO.pure(false)
      }
    } yield passwdIsCorrect

    program.asserting(_ shouldBe true)
  }
}
