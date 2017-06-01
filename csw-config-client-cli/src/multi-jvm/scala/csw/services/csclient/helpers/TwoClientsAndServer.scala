package csw.services.csclient.helpers

import csw.services.location.helpers.NMembersAndSeed
import akka.remote.testconductor.RoleName
import org.jboss.netty.logging.{InternalLoggerFactory, Slf4JLoggerFactory}

class TwoClientsAndServer extends NMembersAndSeed(2) {
  // Fix to avoid 'java.util.concurrent.RejectedExecutionException: Worker has already been shutdown'
  InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory)

  val server: RoleName         = seed
  val Vector(client1, client2) = members
}
