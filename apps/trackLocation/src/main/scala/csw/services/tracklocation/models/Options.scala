package csw.services.tracklocation.models

import java.io.File

/**
  * Command line options ("trackLocation --help" prints a usage message with descriptions of all the options)
  * See val parser below for descriptions of the options.
  */
case class Options(
                    names: List[String] = Nil,
                    command: Option[String] = None,
                    port: Option[Int] = None,
                    appConfigFile: Option[File] = None,
                    delay: Option[Int] = None,
                    noExit: Boolean = false
                  )