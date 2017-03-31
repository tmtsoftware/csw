package csw.services.location.helpers

import akka.actor.ActorSystem
import akka.remote.testconductor.RoleName
import akka.remote.testkit.MultiNodeConfig
import com.typesafe.config.Config
import csw.services.location.internal.Settings

class NMembersAndSeed(n: Int) extends MultiNodeConfig {

  private val settings = Settings()

  def makeSystem(config: Config): ActorSystem = ActorSystem(settings.clusterName, config)

  val seed: RoleName = addRole("seed-0")(settings.asSeed)

  val members: Vector[RoleName] = (1 to n).toVector.map { x =>
    addRole(s"member-$x")(settings.joinLocalSeed)
  }

  private def addRole(name: String)(settings: Settings): RoleName = {
    val node = role(name)
    nodeConfig(node)(settings.config)
    node
  }
}

class OneMemberAndSeed extends NMembersAndSeed(1) {
  val Vector(member1) = members
}

class TwoMembersAndSeed extends NMembersAndSeed(2) {
  val Vector(member1, member2) = members
}
