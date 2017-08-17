package csw.common.framework.internal.configparser

import java.util.NoSuchElementException
import java.util.concurrent.TimeUnit

import com.typesafe.config.ConfigException.WrongType
import com.typesafe.config.{Config, ConfigException}
import csw.common.framework.internal.configparser.Constants._
import csw.common.framework.models.ComponentInfo
import csw.common.framework.models.ComponentInfo.{AssemblyInfo, ContainerInfo, HcdInfo}
import csw.common.framework.models.LocationServiceUsages.{RegisterAndTrackServices, RegisterOnly}
import csw.services.location.models.ComponentType.{Assembly, HCD}
import csw.services.location.models.{ComponentType, Connection, ConnectionType}

import scala.collection.JavaConverters.collectionAsScalaIterableConverter
import scala.concurrent.duration.{DurationInt, FiniteDuration}

object ComponentInfoParser {

  def parse(config: Config): Option[ContainerInfo] =
    try {
      val containerConfig = config.getConfig(CONTAINER)
      val containerName   = parseComponentName(CONTAINER, containerConfig)
      val componentInfoes = parseComponents(containerName.get, containerConfig)
      val registerAs      = parseRegisterAs(containerName.get, containerConfig) // registerAs is mandatory field
      val initialDelay    = parseDuration(containerName.get, INITIAL_DELAY, containerConfig, 0.seconds)
      val creationDelay   = parseDuration(containerName.get, CREATION_DELAY, containerConfig, 0.seconds)
      val lifecycleDelay  = parseDuration(containerName.get, LIFECYCLE_DELAY, containerConfig, 0.seconds)

      Some(
        ContainerInfo(containerName.get,
                      RegisterOnly,
                      registerAs.get,
                      componentInfoes.get,
                      initialDelay,
                      creationDelay,
                      lifecycleDelay)
      )
    } catch {
      case ex: ConfigException.Missing ⇒
        println(s"Missing configuration field >$CONTAINER<")
        None
      case ex: NoSuchElementException ⇒
        None
    }

  private def parseComponentName(name: String, containerConfig: Config): Option[String] =
    try {
      Some(containerConfig.getString(COMPONENT_NAME))
    } catch {
      case ex: ConfigException ⇒
        println(s"Missing configuration field: >$COMPONENT_NAME< in connections for component: $name")
        None
    }

  // Parse the "registerAs" section of the component config
  private def parseRegisterAs(name: String, config: Config): Option[Set[ConnectionType]] =
    try {
      Some(config.getStringList(REGISTER_AS).asScala.map(ctype => ConnectionType.withName(ctype)).toSet)
    } catch {
      case ex: ConfigException.Missing ⇒
        println(s"Missing configuration field: >$REGISTER_AS< for component: $name")
        None
      case ex: ConfigException.WrongType ⇒
        println(
          s"Expected an array of connection types for configuration field: >$REGISTER_AS< for component: $name. e.g. [${ConnectionType.values
            .map(_.name)
            .mkString(",")}]"
        )
        None
      case ex: NoSuchElementException ⇒
        println(s"Invalid connection type for component: $name - ${ex.getMessage}")
        None
    }

  private def parseDuration(containerName: String,
                            delayType: String,
                            containerConfig: Config,
                            defaultDuration: FiniteDuration): FiniteDuration =
    try {
      import scala.concurrent.duration._
      FiniteDuration(containerConfig.getDuration(delayType).getSeconds, TimeUnit.SECONDS)
    } catch {
      case ex: Throwable ⇒
        defaultDuration //logger.debug(Container $delayType for $containerName is missing or not valid, returning: $defaultDuration.)
    }

  private def parseComponents(containerName: String, config: Config) = {

    // Parse the "components" section of the config file
    def parseComponent(name: String, conf: Config): Option[ComponentInfo] =
      try {
        val componentType = conf.getString(COMPONENT_TYPE)
        val componentInfo = ComponentType.withName(componentType) match {
          case Assembly => parseAssembly(name, conf)
          case HCD      => parseHcd(name, conf)
          case _        => None
        }
        componentInfo
      } catch {
        case ex: ConfigException.Missing ⇒
          println(s"Missing configuration field: >$COMPONENT_TYPE< for component: $name")
          None
        case ex: NoSuchElementException ⇒
          println(s"Invalid $COMPONENT_TYPE for component: $name - ${ex.getMessage}")
          None
      }

    try {
      val conf  = config.getConfig(COMPONENTS)
      val names = conf.root.keySet().asScala.toSet

      val entries = names.flatMap { key ⇒
        parseComponent(key, conf.getConfig(key))
      }

      //validation check!!
      if (names.size == entries.size)
        Some(entries)
      else //one of component failed to parse
        None
    } catch {
      case ex: ConfigException.Missing ⇒
        println(s"Missing configuration field: >$COMPONENTS< for component: $containerName")
        None
      case ex: ConfigException.WrongType ⇒
        println(s"Expected config object for configuration field >$COMPONENTS< for component: $containerName ")
        None
    }
  }

  // Parse the "services" section of the component config
  private def parseAssembly(assemblyName: String, conf: Config): Option[AssemblyInfo] = {
    def parseConnections(assemblyName: String, assemblyConfig: Config): Option[Set[Connection]] =
      try {
        Some(assemblyConfig.getStringList(CONNECTIONS).asScala.toSet.map(connection ⇒ Connection.from(connection)))
      } catch {
        case ex: ConfigException.Missing ⇒
          println(s"Missing configuration field: >$CONNECTIONS< for Assembly: $assemblyName")
          None
        case ex: ConfigException.WrongType ⇒
          println(
            s"Expected an array of connections for configuration field >$CONNECTIONS< for Assembly: $assemblyName. e.g. [HCD2A-hcd-akka, HCD2A-hcd-http, HCD2B-hcd-tcp]"
          )
          None
        case ex: IllegalArgumentException ⇒
          println(s"Invalid connection for Assembly: $assemblyName - ${ex.getMessage}")
          None
      }

    try {
      val componentClassName = parseClassName(assemblyName, conf)
      val prefix             = parsePrefix(assemblyName, conf)
      val registerAs         = parseRegisterAs(assemblyName, conf)
      val connections        = parseConnections(assemblyName, conf)

      val assemblyInfo = AssemblyInfo(assemblyName,
                                      prefix.get,
                                      componentClassName.get,
                                      RegisterAndTrackServices,
                                      registerAs.get,
                                      connections.get)
      Some(assemblyInfo)
    } catch {
      case ex: NoSuchElementException ⇒ None
    }
  }

  // Parse the "services" section of the component config
  private def parseHcd(name: String, conf: Config): Option[HcdInfo] =
    try {
      val componentClassName = parseClassName(name, conf)
      val prefix             = parsePrefix(name, conf)
      val registerAs         = parseRegisterAs(name, conf)
      Some(HcdInfo(name, prefix.get, componentClassName.get, RegisterOnly, registerAs.get))
    } catch {
      case ex: NoSuchElementException ⇒ None
    }

  private def parseClassName(name: String, config: Config): Option[String] =
    try {
      Some(config.getString(CLASS))
    } catch {
      case ex: ConfigException ⇒
        println(s"Missing configuration field: >$CLASS< for component: $name")
        None
    }

  private def parsePrefix(name: String, config: Config): Option[String] =
    try {
      Some(config.getString(PREFIX))
    } catch {
      case ex: ConfigException ⇒
        println(s"Missing configuration field: >$PREFIX< for component: $name")
        None
    }
}
