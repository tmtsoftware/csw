package csw.common.framework.internal

import java.util.concurrent.TimeUnit

import com.typesafe.config.Config
import csw.common.framework.models.ComponentInfo
import csw.common.framework.models.ComponentInfo.{AssemblyInfo, ContainerInfo, HcdInfo}
import csw.common.framework.models.LocationServiceUsages.{RegisterAndTrackServices, RegisterOnly}
import csw.services.location.models.ComponentType.{Assembly, HCD}
import csw.services.location.models.{ComponentId, ComponentType, Connection, ConnectionType}
import csw.services.location.models.ConnectionType.AkkaType

import scala.collection.JavaConverters.collectionAsScalaIterableConverter
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.util.{Failure, Success, Try}

object ComponentInfoParser {
  private val CONTAINER       = "container"
  private val COMPONENT_TYPE  = "componentType"
  private val CLASS           = "class"
  private val PREFIX          = "prefix"
  private val CONNECTION_TYPE = "registerAs"
  private val CONNECTIONS     = "connections"
  private val COMPONENT_NAME  = "componentName"
  private val RATE            = "rate"
  private val DELAY           = "delay"
  private val INITIAL_DELAY   = "initialDelay"
  private val CREATION_DELAY  = "creationDelay"
  private val LIFECYCLE_DELAY = "lifecycleDelay"

  // XXX Should these be removed?
  private val DEFAULT_INITIAL_DELAY   = 0.seconds
  private val DEFAULT_CREATION_DELAY  = 0.seconds
  private val DEFAULT_LIFECYCLE_DELAY = 0.seconds

  def parse(config: Config): ContainerInfo =
    (for {
      componentConfigs <- parseConfig(config)
      containerConfig  <- Try(config.getConfig(CONTAINER))
      name             <- parseName("container", containerConfig)
    } yield {
      val initialDelay   = parseDuration(name, INITIAL_DELAY, containerConfig, DEFAULT_INITIAL_DELAY)
      val creationDelay  = parseDuration(name, CREATION_DELAY, containerConfig, DEFAULT_CREATION_DELAY)
      val lifecycleDelay = parseDuration(name, LIFECYCLE_DELAY, containerConfig, DEFAULT_LIFECYCLE_DELAY)
      // For container, if no connectionType, set to Akka
      val registerAs = parseConnTypeWithDefault(name, containerConfig, Set(AkkaType))
      ContainerInfo(name, RegisterOnly, registerAs, componentConfigs, initialDelay, creationDelay, lifecycleDelay)
    }).get

  // Parse the "services" section of the component config
  def parseHcd(name: String, conf: Config): Option[HcdInfo] = {
    val x = for {
      componentClassName <- parseClassName(name, conf)
      prefix             <- parsePrefix(name, conf)
      registerAs         <- parseConnType(name, conf)
      cycle              <- parseRate(name, conf)
    } yield HcdInfo(name, prefix, componentClassName, RegisterOnly, registerAs, cycle)
//    if (x.isFailure) logger.error(s"An error occurred while parsing HCD info for: $name", x.asInstanceOf[Failure[_]].exception)
    x.toOption
  }

  // Parse the "services" section of the component config
  def parseAssembly(name: String, conf: Config): Option[AssemblyInfo] = {
    val x = for {
      componentClassName <- parseClassName(name, conf)
      prefix             <- parsePrefix(name, conf)
      registerAs         <- parseConnType(name, conf)
      connections        <- parseConnections(name, conf)
    } yield AssemblyInfo(name, prefix, componentClassName, RegisterAndTrackServices, registerAs, connections)
    if (x.isFailure) {
      println(s"An error occurred while parsing Assembly info for: $name")
      println(x.asInstanceOf[Failure[_]].exception)
    }
    x.toOption
  }

  // Parse the "services" section of the component config
  private def parseRate(name: String, conf: Config): Try[FiniteDuration] = {
    import scala.concurrent.duration._
    if (!conf.hasPath(RATE))
      Failure(new RuntimeException(s"Missing configuration field: >$RATE< for component: $name"))
    else
      Try(FiniteDuration(conf.getDuration(RATE).getSeconds, TimeUnit.SECONDS))
  }

  private def parseDuration(name: String,
                            configName: String,
                            conf: Config,
                            defaultDuration: FiniteDuration): FiniteDuration = {
    import scala.concurrent.duration._
    val t = Try(FiniteDuration(conf.getDuration(configName).getSeconds, TimeUnit.SECONDS))
    //    if (t.isFailure) logger.debug(s"Container $configName for $name is missing or not valid, returning: $defaultDuration.")
    t.getOrElse(defaultDuration)
  }

  private def parseName(name: String, conf: Config): Try[String] = {
    if (!conf.hasPath(COMPONENT_NAME))
      Failure(
        new RuntimeException(s"Missing configuration field: >$COMPONENT_NAME< in connections for component: $name")
      )
    else Success(conf.getString(COMPONENT_NAME))
  }

  // Parse the "connectionType" section of the component config
  private def parseConnTypeWithDefault(name: String, conf: Config, default: Set[ConnectionType]): Set[ConnectionType] = {
    parseConnType(name, conf).getOrElse(default)
  }

  // Parse the "connectionType" section of the component config
  private def parseConnType(name: String, conf: Config): Try[Set[ConnectionType]] = {
    if (!conf.hasPath(CONNECTION_TYPE))
      Failure(new RuntimeException(s"Missing configuration field: >$CONNECTION_TYPE< for component: $name"))
    else
      Try {
        val set = conf.getStringList(CONNECTION_TYPE).asScala.map(ctype => ConnectionType.withName(ctype)).toSet
        set
      }
  }

  // Parse the "components" section of the config file
  private def parseComponentConfig(name: String, conf: Config): Option[ComponentInfo] = {
    val componentType = conf.getString(COMPONENT_TYPE)
    val info = ComponentType.withName(componentType) match {
      case HCD      => parseHcd(name, conf)
      case Assembly => parseAssembly(name, conf)
      case _        => None
    }
    info
  }

  private def parseClassName(name: String, conf: Config): Try[String] = {
    if (!conf.hasPath(CLASS))
      Failure(new RuntimeException(s"Missing configuration field: >$CLASS< for component: $name"))
    else Success(conf.getString(CLASS))
  }

  private def parsePrefix(name: String, conf: Config): Try[String] = {
    if (!conf.hasPath(PREFIX))
      Failure(new RuntimeException(s"Missing configuration field: >$PREFIX< for component: $name"))
    else Success(conf.getString(PREFIX))
  }
  private def parseConnections(name: String, config: Config): Try[Set[Connection]] = {
    if (!config.hasPath(CONNECTIONS))
      Failure(new RuntimeException(s"Missing configuration field: >$CONNECTIONS< for Assembly: $name"))
    else
      Try {
        // Note: config.getConfigList could throw an exception...
        val list = config.getConfigList(CONNECTIONS).asScala.toList.map { conf: Config =>
          for {
            connName    <- parseName(name, conf)
            componentId <- parseComponentId(connName, conf)
            connTypes   <- parseConnType(connName, conf)
          } yield connTypes.map(connection ⇒ Connection.from(s"${componentId.fullName}-${connection.name}"))
        }
        val failed = list.find(_.isFailure).map(_.asInstanceOf[Failure[_]].exception)
        if (failed.nonEmpty)
          throw failed.get
        else
          list.flatMap(_.get).toSet
      }
  }

  private def parseComponentId(name: String, conf: Config): Try[ComponentId] = {
    if (!conf.hasPath(COMPONENT_TYPE))
      Failure(new RuntimeException(s"Missing configuration field: >$COMPONENT_TYPE< for component: $name"))
    else {
      val componentType = ComponentType.withName(conf.getString(COMPONENT_TYPE))
      Success(ComponentId(name, componentType))
    }
  }

  private def parseConfig(config: Config): Try[Set[ComponentInfo]] = {
    Try {
      val conf  = config.getConfig("container.components")
      val names = conf.root.keySet().asScala.toList
      val entries = for {
        key   <- names
        value <- parseComponentConfig(key, conf.getConfig(key))
      } yield value
      Set(entries: _*)
    }
  }
}
