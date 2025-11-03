package com.rockthejvm.jobsboard.http.routes

import cats.effect.Concurrent
import cats.implicits.*
import com.rockthejvm.jobsboard.core.Jobs
import com.rockthejvm.jobsboard.domain.job.*
import com.rockthejvm.jobsboard.domain.pagination.Pagination
import com.rockthejvm.jobsboard.http.response.FailureResponse
import com.rockthejvm.jobsboard.http.validation.syntax.*
import io.circe.generic.auto.*
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.server.Router
import org.typelevel.log4cats.Logger

import java.util.UUID

class JobRoutes[F[_]: Concurrent: Logger] private (jobs: Jobs[F]) extends HttpValidationDsl[F] {
  private object OffsetQueryParam extends OptionalQueryParamDecoderMatcher[Int]("offset")
  private object LimitQueryParam  extends OptionalQueryParamDecoderMatcher[Int]("limit")

  // POST /jobs?offset=x&limit=< { filters } // TODO add query params and filters
  private val allJobsRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ POST -> Root :? LimitQueryParam(limit) +& OffsetQueryParam(offset) =>
      for {
        filter   <- request.as[JobFilter]
        jobsList <- jobs.all(filter, Pagination(limit, offset))
        response <- Ok(jobsList)
      } yield response
  }

  // GET /jobs/uuid
  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / UUIDVar(id) =>
    jobs.find(id).flatMap {
      case Some(job) => Ok(job)
      case None      => NotFound(FailureResponse(s"Job with $id not found."))
    }
  }
  
  // POST /jobs/create { jobIn
  import com.rockthejvm.jobsboard.logging.syntax.*
  private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ POST -> Root / "create" =>
      request.validate[JobInfo] { jobInfo =>
        for {
          jobId    <- jobs.create("TODO@rockthejvm.com", jobInfo)
          response <- Ok(jobId)
        } yield response
      }
  }

  // PUT /jobs/uuid { jobInfo }
  private val updateJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ PUT -> Root / UUIDVar(id) =>
      request.validate[JobInfo] { jobInfo =>
        for {
          mayBeNewJob <- jobs.update(id, jobInfo)
          response    <- mayBeNewJob match {
            case Some(job) => Ok()
            case None      => NotFound(FailureResponse(s"Cannot update job $id:  not found"))
          }
        } yield response
      }
  }

  // DELETE /job/uuid
  private val deleteJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case DELETE -> Root / UUIDVar(id) =>
      jobs.find(id).flatMap {
        case Some(job) =>
          for {
            _        <- jobs.delete(id)
            response <- Ok()
          } yield response
        case None      => NotFound(FailureResponse(s"Cannot delete job $id:  not found"))
      }
  }

  val routes: HttpRoutes[F] = Router(
    "/jobs" -> {
      allJobsRoutes <+> findJobRoute <+> createJobRoute <+> updateJobRoute <+> deleteJobRoute
    }
  )
}
object JobRoutes {
  def apply[F[_]: Concurrent: Logger](jobs: Jobs[F]) = new JobRoutes[F](jobs)
}
