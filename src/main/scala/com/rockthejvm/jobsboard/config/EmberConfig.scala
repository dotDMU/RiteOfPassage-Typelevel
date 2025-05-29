package com.rockthejvm.jobsboard.config

import com.comcast.ip4s.{Host, Port}
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert

// TypeClass Derivation
// die Bibliothek pureconfig.ConfigReader kann automatisch den given anbieten
// generates a 
// given configReader: ConfigReader[EmberConfig]

final case class EmberConfig (host: Host, port: Port) derives ConfigReader

object EmberConfig {
  // need a given ConfigReader[Host] + ConfigReader[Port] => so the compiler generates ConfigReader[EmberConfig]
  given hostReader: ConfigReader[Host] = ConfigReader[String].emap { hostString => 
    Host.fromString(hostString) match {
      case None => 
        Left(CannotConvert(hostString, Host.getClass.toString,  s"Invalid host string: $hostString"))
      case Some(host) => 
        Right(host)
    }
  }
  
  given portReader: ConfigReader[Port] = ConfigReader[Int].emap { portInt =>
    Port
      .fromInt(portInt)
      .toRight(CannotConvert(portInt.toString, Port.getClass.toString,  s"Invalid port number: $portInt"))
  }
}