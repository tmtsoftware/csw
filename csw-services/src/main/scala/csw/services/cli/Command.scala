package csw.services.cli

import caseapp.{CommandName, HelpMessage, ExtraName => Short}

sealed trait Command

object Command {
  @CommandName("start")
  @HelpMessage("starts all the CSW services by default if no other option is provided")
  final case class Start(
      @HelpMessage("start location server")
      @Short("l")
      location: Boolean = false,
      @HelpMessage("start config server")
      @Short("c")
      config: Boolean = false,
      @HelpMessage("start event server")
      @Short("e")
      event: Boolean = false,
      @HelpMessage("start alarm server")
      @Short("a")
      alarm: Boolean = false,
      @HelpMessage(
        "start database service, set 'PGDATA' env variable where postgres is installed e.g. for mac: /usr/local/var/postgres"
      )
      @Short("d")
      database: Boolean = false,
      @HelpMessage("start auth/aas service")
      @Short("k")
      auth: Boolean = false,
      @HelpMessage("name of the inside interface")
      @Short("i")
      interfaceName: Option[String],
      @HelpMessage("name of the outside interface")
      @Short("o")
      outsideInterfaceName: Option[String]
  ) extends Command

  object Start {
    def apply(
        location: Boolean = false,
        config: Boolean = false,
        event: Boolean = false,
        alarm: Boolean = false,
        database: Boolean = false,
        auth: Boolean = false,
        insideInterfaceName: Option[String] = None,
        outsideInterfaceName: Option[String] = None
    ): Start = {
      if (location || config || event || alarm || database || auth) {
        // always start location server if explicitly started or any other service is started
        new Start(true, config, event, alarm, database, auth, insideInterfaceName, outsideInterfaceName)
      }
      // mark all flags=true when no option is provided to start command
      else new Start(true, true, true, true, false, true, insideInterfaceName, outsideInterfaceName)
    }
  }

}
