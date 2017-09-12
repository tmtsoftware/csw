package csw.apps.containercmd.sample

import csw.apps.containercmd.ContainerCmd

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

object ContainerCmdApp extends App {
  Await.result(ContainerCmd.start(args), 5.seconds)
}
