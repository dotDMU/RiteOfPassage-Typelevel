package com.rockthejvm.jobsboard.http.routes

import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import com.rockthejvm.jobsboard.core.*
import com.rockthejvm.jobsboard.domain.job.{Job, JobFilter, JobInfo}
import com.rockthejvm.jobsboard.domain.pagination.Pagination
import com.rockthejvm.jobsboard.fixtures.JobFixture
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.*
import org.http4s.implicits.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.util.UUID

class JobRoutesSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Http4sDsl[IO]
    with JobFixture {

  /** PREP
    */

  val jobs: Jobs[IO] = new Jobs[IO] {
    override def create(ownerEmail: String, jobInfo: JobInfo): IO[UUID] = {
      IO.pure(NewJobUuid)
    }
    override def all(): IO[List[Job]]                                   = {
      IO.pure(List(AwesomeJob))
    }

    override def all(filter: JobFilter, pagination: Pagination): IO[List[Job]] = {
      if (filter.remote) IO.pure(List())
      else IO.pure(List(AwesomeJob))
    }
    override def find(id: UUID): IO[Option[Job]]                               = {
      if (id == AwesomeJobUuid)
        IO.pure((Some(AwesomeJob)))
      else
        IO.pure(None)
    }
    override def update(id: UUID, jobInfo: JobInfo): IO[Option[Job]]           = {
      if (id == AwesomeJobUuid)
        IO.pure(Some(UpdatedAwesomeJob))
      else
        IO.pure(None)
    }
    override def delete(id: UUID): IO[Int]                                     = {
      if (id == AwesomeJobUuid)
        IO.pure(1)
      else
        IO.pure(0)
    }
  }

  given logger: Logger[IO]       = Slf4jLogger.getLogger[IO]
  val jobsRoutes: HttpRoutes[IO] = JobRoutes[IO](jobs).routes

  /** TESTS
    */
  "JobRoutes" - {
    "should return a job with the given id" in {
      // code under test
      for {
        // simulate an HTTP request
        response  <- jobsRoutes.orNotFound.run(
          Request(method = Method.GET, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
        )
        retrieved <- response.as[Job]

      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe AwesomeJob
      }
    }

    "should return all jobs" in {
      for {
        response  <- jobsRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/jobs")
            .withEntity(JobFilter())
        )
        retrieved <- response.as[List[Job]]

      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe List(AwesomeJob)
      }
    }

    "should return all jobs that satisfy a filter" in {
      for {
        response <- jobsRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/jobs")
            .withEntity(JobFilter(remote = true))
        )
        retrieved <- response.as[List[Job]]

      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe List()
      }
    }

    "should create a new job" in {
      for {
        response  <- jobsRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/jobs/create")
            .withEntity(AwesomeJob.jobInfo)
        )
        retrieved <- response.as[UUID]

      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe NewJobUuid
      }
    }

    "should only update a job that exists" in {
      for {
        responseOk      <- jobsRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
            .withEntity(UpdatedAwesomeJob.jobInfo)
        )
        responseInvalid <- jobsRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/jobs/843df718-ec6e-4d49-9289-00000000000")
            .withEntity(UpdatedAwesomeJob.jobInfo)
        )
      } yield {
        responseOk.status shouldBe Status.Ok
        responseInvalid.status shouldBe Status.NotFound
      }
    }

    "should only delete a job that exists" in {
      for {
        responseOk      <- jobsRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
            .withEntity(UpdatedAwesomeJob.jobInfo)
        )
        responseInvalid <- jobsRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/jobs/843df718-ec6e-4d49-9289-00000000000")
            .withEntity(UpdatedAwesomeJob.jobInfo)
        )
      } yield {
        responseOk.status shouldBe Status.Ok
        responseInvalid.status shouldBe Status.NotFound
      }
    }
  }

}
