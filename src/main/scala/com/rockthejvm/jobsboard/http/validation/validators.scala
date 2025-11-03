package com.rockthejvm.jobsboard.http.validation

import cats.*
import cats.implicits.*
import cats.data.*
import cats.data.Validated.*
import com.rockthejvm.jobsboard.domain.job.JobInfo

import java.net.URI
import scala.util.{Try, Success, Failure}

object validators {
  sealed trait ValidationFailure(val errorMessage: String)
  case class EmptyField(fieldName: String) extends ValidationFailure(s"'$fieldName' is empty'")
  case class InvalidUrl(fieldName: String)
      extends ValidationFailure(s"'$fieldName' is no valid URL'")

  type ValidationResult[A] = ValidatedNel[ValidationFailure, A]

  trait Validator[A] {
    def validate(value: A): ValidationResult[A]
  }

  private def validateRequired[A](field: A, fieldName: String)(
      required: A => Boolean
  ): ValidationResult[A] = {
    if (required(field)) field.validNel
    else EmptyField(fieldName).invalidNel
  }

  def validateUrl(field: String, fieldName: String): ValidationResult[String] = {
    Try(URI(field).toURL) match {
      case Success(_) => field.validNel
      case Failure(e) => InvalidUrl(fieldName).invalidNel
    }
  }

  given jobInfoValidator: Validator[JobInfo] = (jobInfo: JobInfo) => {
    val JobInfo(
      company,     // should not be empty
      title,       // should not be empty
      description, // should not be empty
      externalUrl, // should be valid url
      remote,
      location,    // should not be empty
      salaryLo,
      salaryHi,
      currency,
      country,
      tags,
      image,
      seniority,
      other
    ) = jobInfo

    val validCompany     = validateRequired(company, "company")(_.nonEmpty)
    val validTitel       = validateRequired(title, "title")(_.nonEmpty)
    val validDescription = validateRequired(description, "description")(_.nonEmpty)
    val validExternalUrl = validateUrl(externalUrl, "externalUrl")
    val validLocation    = validateRequired(location, "location")(_.nonEmpty)

    (
      validCompany,       // company
      validTitel,         // title
      validDescription,   // description
      validExternalUrl,   // externalUrl
      remote.validNel,    // remote
      validLocation,      // location
      salaryLo.validNel,  // salaryLo
      salaryHi.validNel,  // salaryHi
      currency.validNel,  // currency
      country.validNel,   // country
      tags.validNel,      // tags
      image.validNel,     // image
      seniority.validNel, // seniority
      other.validNel      // other
    ).mapN(JobInfo.apply) // ValidatedNel[ValidationFailure,JobInfo]

  }
}
