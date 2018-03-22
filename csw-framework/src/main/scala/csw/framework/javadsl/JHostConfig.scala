package csw.framework.javadsl

import csw.framework.deploy.hostconfig.HostConfig

/**
 * Helper instance for Java to start [[csw.framework.deploy.hostconfig.HostConfig]] app
 */
object JHostConfig {

  /**
   * Utility for starting multiple Containers on a single host machine
   *
   * @param name the name to be used for the main app which uses this utility
   * @param args the command line args accepted in the main app which uses this utility
   */
  def start(name: String, args: Array[String]): Unit = HostConfig.start(name: String, args: Array[String])

}
