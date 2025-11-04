package com.rockthejvm.jobsboard.core

import cats.data.EitherT
import cats.effect.kernel.*
import cats.implicits.*
import cats.effect.*

import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.PasswordHasher
import tsec.passwordhashers.jca.BCrypt

import com.rockthejvm.jobsboard.domain.auth.NewPasswordInfo
import com.rockthejvm.jobsboard.domain.security.*
import com.rockthejvm.jobsboard.domain.user.{NewUserInfo, User}
import org.typelevel.log4cats.Logger

trait Auth[F[_]] {
  def login(email: String, password: String): F[Option[JwtToken]]
  def signUp(newUserInfo: NewUserInfo): F[Option[User]]
  def changePassword(email: String, newPasswordInfo: NewPasswordInfo): EitherT[F, MyError, User]
}

class LiveAuth[F[_]: Sync: MonadCancelThrow: Logger] private (
    users: Users[F],
    authenticator: Authenticator[F]
) extends Auth[F] {
  override def login(email: String, password: String): F[Option[JwtToken]] = {
    for {
      // find the user -> return none if none found
      maybeUser          <- users.find(email)
      // check password (hashed)
      // wir benötigen Option[User], jedoch checkpwBool gibt F[Boolean], .filter returned Option, nach das ganze in Effekt damit wir F[Option[User]] erhalten, dazu filterA
      maybeValidatedUser <- maybeUser.filterA(user =>
        BCrypt.checkpwBool[F](
          password,
          PasswordHash[BCrypt](user.hashedPassword)
        )
      )
      // nur mit .map wäre es Option[User].map(User => F[JwtToken]) => Option[F[JWTToken]], .traverse packt den Effekt nach vorn
      maybeJwtToken      <- maybeValidatedUser.traverse(user => authenticator.create(user.email))
    } yield maybeJwtToken
  }

  override def signUp(newUserInfo: NewUserInfo): F[Option[User]] = ???

  override def changePassword(
      email: String,
      newPasswordInfo: NewPasswordInfo
  ): EitherT[F, MyError, User] = ???
}

object LiveAuth {
  def apply[F[_]: Sync: MonadCancelThrow: Logger](
      users: Users[F],
      authenticator: Authenticator[F]
  ): F[LiveAuth[F]] =
    new LiveAuth[F](users, authenticator).pure[F]
}
