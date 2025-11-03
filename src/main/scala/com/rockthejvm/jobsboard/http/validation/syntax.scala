package com.rockthejvm.jobsboard.http.validation

import cats.*
import cats.data.*
import cats.data.Validated.*
import cats.implicits.*
import com.rockthejvm.jobsboard.http.response.FailureResponse
import com.rockthejvm.jobsboard.http.validation.validators.{ValidationResult, Validator}
import com.rockthejvm.jobsboard.logging.syntax.*
import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, Request, Response}
import org.typelevel.log4cats.Logger

object syntax {
  def validateEntity[A](entity: A)(using validator: Validator[A]): ValidationResult[A] = {
    validator.validate(entity)
  }
  trait HttpValidationDsl[F[_]: MonadThrow: Logger] extends Http4sDsl[F] {

    extension (req: Request[F])
      def validate[A: Validator](
          serverLogicIfFailed: A => F[Response[F]]
      )(using EntityDecoder[F, A]): F[Response[F]] =
        req
          .as[A]
          .logError(e => s"PArsing payload failed: $e")
          .map(validateEntity) // F[ValidationResult[A]]
          .flatMap {
            case Valid(entity)   =>
              serverLogicIfFailed(entity)
            case Invalid(errors) =>
              BadRequest(FailureResponse(errors.toList.map(_.errorMessage).mkString(", ")))
          }
  }
}
