package csw.services.location.helpers

import akka.actor.ActorSystem
import akka.remote.testconductor.RoleName
import akka.remote.testkit.MultiNodeConfig
import com.typesafe.config.Config
import csw.services.location.internal.Settings

class LSNodeConfig extends MultiNodeConfig {
  private val settings = Settings()

  def makeSystem(config: Config): ActorSystem = ActorSystem(settings.clusterName, config)

  val seed: RoleName = seedRole("seed-0")

  def memberRole(name: String): RoleName = addRole(name)(settings.joinLocalSeed)

  private def seedRole(name: String): RoleName = addRole(name)(settings.asSeed)

  private def addRole(name: String)(settings: Settings): RoleName = {
    val node = role(name)
    nodeConfig(node)(settings.config)
    node
  }
}

object LSNodeConfig {

  class OneMemberAndSeed extends LSNodeConfig {
    val member1: RoleName = memberRole("member-1")
  }

  class TwoMembersAndSeed extends OneMemberAndSeed {
    val member2: RoleName = memberRole("member-2")
  }

  class NMembersAndSeed(n: Int) extends LSNodeConfig {
    val members: Vector[RoleName] = (1 to n).map(x ⇒ memberRole(s"member-$x")).toVector
  }

}
