package csw.config.cli.helpers

import csw.location.helpers.NMembersAndSeed
import akka.remote.testconductor.RoleName
import io.netty.util.internal.logging.{InternalLoggerFactory, Slf4JLoggerFactory}

class TwoClientsAndServer extends NMembersAndSeed(2) {
  // Fix to avoid 'java.util.concurrent.RejectedExecutionException: Worker has already been shutdown'
  InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE)

  val server: RoleName = seed
  val (client1, client2) = members match {
    case Vector(client1, client2) => (client1, client2)
    case x                        => throw new MatchError(x)
  }

}
