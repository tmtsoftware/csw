package csw.services.cli

import caseapp.{CommandName, HelpMessage, ExtraName => Short}

sealed trait Command

object Command {
  @CommandName("start")
  @HelpMessage("start csw services")
  final case class Start(
      @HelpMessage("start all services")
      @Short("a")
      all: Boolean = false,
      @HelpMessage("start config server")
      @Short("c")
      config: Boolean = false,
      @HelpMessage("start event server")
      @Short("e")
      event: Boolean = false,
      @HelpMessage("start alarm server")
      @Short("r")
      alarm: Boolean = false,
      @HelpMessage(
        "start database service, set 'PGDATA' env variable where postgres is installed e.g. for mac: /usr/local/var/postgres"
      )
      @Short("d")
      database: Boolean = false,
      @HelpMessage("start auth/aas service")
      @Short("k")
      auth: Boolean = false,
      @HelpMessage("name of the interface")
      @Short("i")
      interfaceName: Option[String]
  ) extends Command

  object Start {
    def apply(
        all: Boolean = false,
        config: Boolean = false,
        event: Boolean = false,
        alarm: Boolean = false,
        database: Boolean = false,
        auth: Boolean = false,
        interfaceName: Option[String] = None
    ): Start =
      if (all) new Start(all, true, true, true, true, true, interfaceName)
      else new Start(all, config, event, alarm, database, auth, interfaceName)
  }

}
