package csw.framework.javadsl

import csw.apps.hostconfig.HostConfig

object JHostConfig {

  /**
   * Utility for starting multiple Containers on a single host machine
   * @param name              The name to be used for the main app which uses this utility
   * @param args              The command line args accepted in the main app which uses this utility
   */
  def start(name: String, args: Array[String]): Unit = HostConfig.start(name: String, args: Array[String])

}
