package com.rockthejvm.jobsboard.http.routes

import cats.effect.Concurrent
import cats.{Monad, MonadThrow}
import cats.implicits.*
import com.rockthejvm.jobsboard.domain.job.*
import com.rockthejvm.jobsboard.http.response.FailureResponse
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.circe.CirceEntityCodec.*
import io.circe.generic.auto.*

import java.util.UUID
import scala.collection.mutable

class JobRoutes[F[_] : Concurrent] private extends Http4sDsl [F]{

  // "database"
  private val database = mutable.Map[UUID, Job]()

  // POST /jobs?offset=x&limit=< { filters } // TODO add query params and filters
  private val allJobsRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case POST -> Root =>
      Ok(database.values)
  }

  // GET /jobs/uuid
  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / UUIDVar(id) =>
      database.get(id) match {
        case Some(job) => Ok(job)
        case None => NotFound(FailureResponse(s"Job with $id not found."))
      }
  }

  // POST /jobs { jobInfo }
  private def createJob(jobInfo: JobInfo): F[Job] = {
    Job (
      id = UUID.randomUUID(),
      date = System.currentTimeMillis(),
      ownerEmail = "TODO",
      jobInfo = jobInfo,
      active = true
    ).pure[F]
  }

  private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ POST -> Root / "create" =>
      for {
        jobInfo <- request.as[JobInfo]
        job <- createJob(jobInfo)
        response <- Created(job.id)
      } yield response
  }

  // PUT /jobs/uuid { jobInfo }
  private val updateJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ PUT -> Root / UUIDVar(id) =>
      database.get(id) match {
        case Some(job) =>
          for {
            jobInfo <- request.as[JobInfo]
            _ <- database.put(id, job.copy(jobInfo = jobInfo)).pure[F]
            response <- Ok()
          } yield response
        case None => NotFound(FailureResponse(s"Cannot update job $id:  not found"))
      }
  }

  // DELETE /job/uuid
  private val deleteJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case DELETE -> Root / UUIDVar(id) =>
      database.get(id) match {
        case Some(job) =>
          for {
            _ <- database.remove(id).pure[F]
            response <- Ok()
          } yield response
        case None => NotFound(FailureResponse(s"Cannot delete job $id:  not found"))
      }  }

  val routes: HttpRoutes[F] = Router(
    "/jobs" -> { allJobsRoutes <+> findJobRoute <+> createJobRoute <+> updateJobRoute <+> deleteJobRoute}
  )
}
  object JobRoutes {
    def apply[F[_]: Concurrent] = new JobRoutes[F]
  }