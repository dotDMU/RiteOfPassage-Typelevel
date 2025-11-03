package com.rockthejvm.jobsboard.modules

import cats.implicits.*
import cats.*
import cats.effect.*
import cats.effect.kernel.Async
import com.rockthejvm.jobsboard.core.Jobs
import com.rockthejvm.jobsboard.core.LiveJobs
import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger

final class Core[F[_]] private (val jobs: Jobs[F])

object Core {

  def apply[F[_]: Async: Logger](xa: Transactor[F]): Resource[F, Core[F]] =
    Resource
  .eval(LiveJobs[F](xa))
    .map(jobs => new Core(jobs))

}
