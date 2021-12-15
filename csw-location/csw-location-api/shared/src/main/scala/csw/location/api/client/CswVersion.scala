package csw.location.api.client

import csw.location.api.models.Metadata
import csw.prefix.models.Prefix

trait CswVersion {
  def check(metadata: Metadata, prefix: Prefix): Unit
  def get: String
}

object CswVersion {
  def noOp: CswVersion = new CswVersion {
    override def check(metadata: Metadata, prefix: Prefix): Unit = ()
    override def get: String                                     = "no-version"
  }
}
