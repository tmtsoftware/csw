package csw.apps.clusterseed.admin.internal

import com.typesafe.config.Config

class Settings(config: Config) {

  private val `csw-cluster-seed` = config.getConfig("csw-cluster-seed")

  def `admin-port`: Int = `csw-cluster-seed`.getInt("admin-port")
}
