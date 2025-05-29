package com.rockthejvm.jobsboard.config

import cats.MonadThrow
import cats.implicits.*
import pureconfig.error.ConfigReaderException
import pureconfig.{ConfigReader, ConfigSource}

import scala.reflect.ClassTag

// This defines an extension method in Scala 3: you're adding a new method (loadF) to the type ConfigSource without modifying its original definition.
//This means you can now write:
//ConfigSource.default.at("ember-config").loadF[F, MyConfig]

//def loadF[F[_], A](using
//  reader: ConfigReader[A],
//  F: MonadThrow[F],
//  ct: ClassTag[A]
//): F[A]

//This is a generic, monadic method that:
//    Abstracts over effect types F[_] (like IO, Future, etc.)
//    Loads a configuration value of type A
//    Uses type classes for:
//        ConfigReader[A] → How to read type A from config
//        MonadThrow[F] → How to create values or errors inside the effect
//        ClassTag[A] → Required to get runtime type info (for PureConfig)

object syntax {
  extension (source: ConfigSource)
    def loadF[F[_], A](using reader: ConfigReader[A], F: MonadThrow[F], ct: ClassTag[A]): F[A] = {
      F.pure(source.load[A]).flatMap {
        case Left(errors) => F.raiseError[A](ConfigReaderException(errors))
        case Right(value) => F.pure(value)
      }
    }
}

//F.pure(source.load[A]).flatMap {
//  case Left(errors) => F.raiseError[A](ConfigReaderException(errors))
//  case Right(value) => F.pure(value)
//}
//
//This is a little workaround: since source.load[A] returns an Either, and you're working inside an effect F[_], the method does:
//    F.pure(source.load[A]) → lift the Either into F[Either[...]]
//    flatMap → extract and react to Left vs Right
//    On Left(errors) → raise error inside F
//    On Right(value) → return the value inside F

// ---------------------------------------------------------
// Summary
//  object syntax	Namespace for clean imports
//  extension (source: ConfigSource)	Adds method to existing type
//  loadF[F[_], A]	Generic, monadic config loader
//  using ConfigReader, MonadThrow, ClassTag	Type class-based dependencies
//  F.pure(...).flatMap	Lifts Either into monad and handles error/value