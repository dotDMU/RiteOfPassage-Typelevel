package com.rockthejvm.jobsboard.http.routes

import com.rockthejvm.jobsboard.fixtures.JobFixture

import cats.effect.*
import cats.implicits.*
import org.scalatest.freespec.AsyncFreeSpec
import org.http4s.dsl.Http4sDsl
import org.scalatest.matchers.must.Matchers

class JobRoutesSpec extends AsyncFreeSpec with Matchers with Http4sDsl[IO] with JobFixture {
  val x = 2
}
