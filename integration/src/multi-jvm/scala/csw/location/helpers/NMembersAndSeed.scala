package csw.location.helpers

import akka.actor
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.remote.testconductor.RoleName
import akka.remote.testkit.MultiNodeConfig
import com.typesafe.config.{Config, ConfigFactory}
import csw.location.server.commons.{ClusterAwareSettings, ClusterSettings}
import io.netty.util.internal.logging.{InternalLoggerFactory, Slf4JLoggerFactory}

class NMembersAndSeed(n: Int) extends MultiNodeConfig {

  val settings = ClusterAwareSettings

  // Fix to avoid 'java.util.concurrent.RejectedExecutionException: Worker has already been shutdown'
  InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE)

  def makeSystem(config: Config): actor.ActorSystem = ActorSystem(SpawnProtocol(), settings.clusterName, config).toClassic

  val seed: RoleName = addRole("seed")(settings.onPort(3552))

  val members: Vector[RoleName] = (1 to n).toVector.map { x =>
    addRole(s"member-$x")(settings.joinLocal(3552))
  }

  commonConfig(ConfigFactory.parseString("akka.loggers = [csw.logging.compat.AkkaLogger]"))

  private def addRole(name: String)(settings: ClusterSettings): RoleName = {
    val node = role(name)
    nodeConfig(node)(settings.withEntries(sys.env).config)
    node
  }
}

class OneMemberAndSeed extends NMembersAndSeed(1) {
  val member: RoleName = members match {
    case Vector(member) => member
    case x              => throw new MatchError(x)
  }

}

class TwoMembersAndSeed extends NMembersAndSeed(2) {
  val (member1, member2) = members match {
    case Vector(member1, member2) => (member1, member2)
    case x                        => throw new MatchError(x)
  }
}
