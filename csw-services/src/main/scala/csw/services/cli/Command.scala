package csw.services.cli

import caseapp.{CommandName, HelpMessage, ExtraName => Short}

sealed trait Command

object Command {
  @CommandName("start")
  @HelpMessage("starts all the CSW services by default if no other option is provided")
  final case class Start(
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
      insideInterfaceName: Option[String],
      @HelpMessage("name of the outside interface")
      @Short("o")
      outsideInterfaceName: Option[String]
  ) extends Command

  object Start {
    def apply(
        config: Boolean = false,
        event: Boolean = false,
        alarm: Boolean = false,
        database: Boolean = false,
        auth: Boolean = false,
        insideInterfaceName: Option[String] = None,
        outsideInterfaceName: Option[String] = None
    ): Start =
      // mark all flags=true when no option is provided to start command
      if (config || event || alarm || database || auth)
        new Start(config, event, alarm, database, auth, insideInterfaceName, outsideInterfaceName)
      else new Start(true, true, true, true, true, insideInterfaceName, outsideInterfaceName)
  }

}
