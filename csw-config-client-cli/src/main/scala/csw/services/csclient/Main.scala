package csw.services.csclient

import com.typesafe.scalalogging.LazyLogging
import csw.services.csclient.cli.{ArgsParser, ClientCliWiring}
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings}

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

class Main(clusterSettings: ClusterSettings) {
  def start(args: Array[String]): Unit =
    ArgsParser.parse(args).foreach { options ⇒
      val wiring = new ClientCliWiring(clusterSettings)
      import wiring._
      try {
        options.op match {
          //adminApi
          case "create"             ⇒ commandLineRunner.create(options)
          case "update"             ⇒ commandLineRunner.update(options)
          case "get"                ⇒ commandLineRunner.get(options)
          case "delete"             ⇒ commandLineRunner.delete(options)
          case "list"               ⇒ commandLineRunner.list(options)
          case "history"            ⇒ commandLineRunner.history(options)
          case "historyActive"      ⇒ commandLineRunner.history(options)
          case "setActiveVersion"   ⇒ commandLineRunner.setActiveVersion(options)
          case "resetActiveVersion" ⇒ commandLineRunner.resetActiveVersion(options)
          case "getActiveVersion"   ⇒ commandLineRunner.getActiveVersion(options)
          case "getActiveByTime"    ⇒ commandLineRunner.getActiveByTime(options)
          case "getMetadata"        ⇒ commandLineRunner.getMetadata(options)
          //clientApi
          case "exists"    ⇒ commandLineRunner.exists(options)
          case "getActive" ⇒ commandLineRunner.getActive(options)
          case x           ⇒ throw new RuntimeException(s"Unknown operation: $x")
        }
      } finally {
        Await.result(actorRuntime.shutdown(), 10.seconds)
      }
    }
}

object Main extends App with LazyLogging {
  if (ClusterAwareSettings.seedNodes.isEmpty) {
    logger.error(
      "clusterSeeds setting is not specified either as env variable or system property. Please check online documentation for this set-up."
    )
  } else {
    new Main(ClusterAwareSettings).start(args)
  }
}
