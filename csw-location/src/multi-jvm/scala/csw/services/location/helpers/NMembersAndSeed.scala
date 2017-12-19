package csw.services.location.helpers

import java.util.UUID

import akka.actor.ActorSystem
import akka.remote.testconductor.RoleName
import akka.remote.testkit.MultiNodeConfig
import com.typesafe.config.{Config, ConfigFactory}
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings}
import org.jboss.netty.logging.{InternalLoggerFactory, Slf4JLoggerFactory}

import scala.concurrent.duration.DurationInt

class NMembersAndSeed(n: Int) extends MultiNodeConfig {

  val settings = ClusterAwareSettings

  // Fix to avoid 'java.util.concurrent.RejectedExecutionException: Worker has already been shutdown'
  InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory)

  def makeSystem(config: Config): ActorSystem = ActorSystem(settings.clusterName, config)

  val seed: RoleName = addRole("seed")(settings.onPort(3552))

  val members: Vector[RoleName] = (1 to n).toVector.map { x =>
    addRole(s"member-$x")(settings.joinLocal(3552))
  }

  commonConfig(ConfigFactory.parseString("akka.loggers = [csw.services.logging.compat.AkkaLogger]"))

  private def addRole(name: String)(settings: ClusterSettings): RoleName = {
    val node = role(name)
    nodeConfig(node)(settings.withEntries(sys.env).config)
    node
  }
}

class OneMemberAndSeed extends NMembersAndSeed(1) {
  val Vector(member) = members
}

class TwoMembersAndSeed extends NMembersAndSeed(2) {
  val Vector(member1, member2) = members
}

class OneMemberAndSeedForPerf extends NMembersAndSeed(1) {
  val Vector(member) = members
  val barrierTimeout = 5.minutes

  val cfg = ConfigFactory.parseString(s"""
     csw-logging.logLevel = debug
     # for serious measurements you should increase the totalMessagesFactor (20)
     akka.test.MaxThroughputSpec.totalMessagesFactor = 10.0
     akka.test.MaxThroughputSpec.real-message = off
     akka.test.MaxThroughputSpec.actor-selection = off
     akka {
       loglevel = debug
       log-dead-letters = 100
       # avoid TestEventListener
       loggers = ["csw.services.logging.compat$$AkkaLogger"]
       testconductor.barrier-timeout = ${barrierTimeout.toSeconds}s
       actor {
         provider = remote
         serialize-creators = false
         serialize-messages = false
         allow-java-serialization = on
         serializers {
            kryo = "com.twitter.chill.akka.AkkaSerializer"
         }
         serialization-bindings {
            "csw.messages.TMTSerializable" = kryo
         }
       }
     }
     akka.remote.default-remote-dispatcher {
       fork-join-executor {
         # parallelism-factor = 0.5
         parallelism-min = 4
         parallelism-max = 4
       }
       # Set to 10 by default. Might be worthwhile to experiment with.
       # throughput = 100
     }
     """)

  commonConfig(
    debugConfig(on = false)
      .withFallback(cfg)
      .withFallback(RemotingMultiNodeSpec.commonConfig)
      .withFallback(ConfigFactory.parseString("akka.loggers = [csw.services.logging.compat.AkkaLogger]"))
  )

  override def makeSystem(config: Config): ActorSystem = ActorSystem(settings.clusterName, config)
}

object RemotingMultiNodeSpec {

  def commonConfig: Config =
    ConfigFactory.parseString(s"""
        akka.actor.warn-about-java-serializer-usage = off
        akka.remote.artery.advanced.flight-recorder {
          enabled=on
          destination=target/flight-recorder-${UUID.randomUUID().toString}.afr
        }
      """)

}
