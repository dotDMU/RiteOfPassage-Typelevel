package com.rockthejvm.jobsboard.core

sealed trait MyError {
  def message: String
}

object MyError {
  final case class StandardError(message: String) extends MyError
}
