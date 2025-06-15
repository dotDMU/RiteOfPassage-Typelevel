package com.rockthejvm.jobsboard.modules

import com.rockthejvm.jobsboard.domain.job.{Job, JobInfo}

import java.util.UUID

trait Jobs[F[_]] {

//  "algebra"
//  CRUD
  def create(ownerEmail: String, jobInfo: JobInfo): F[UUID]
  def all(): F[List[Job]]
  def find(id: UUID): F[Option[Job]]


}
