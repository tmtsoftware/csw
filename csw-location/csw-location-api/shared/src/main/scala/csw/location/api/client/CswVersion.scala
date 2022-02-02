package csw.location.api.client

import csw.location.api.models.Metadata
import csw.prefix.models.Prefix

trait CswVersion {
  def check(metadata: Metadata, prefix: Prefix): Boolean
  def get: String
}

object CswVersion {
  // this method return a no-op instance of CswVersion
  // which is to be used for scala-js
  def noOp: CswVersion = new CswVersion {
    override def check(metadata: Metadata, prefix: Prefix): Boolean = true
    override def get: String                                        = "no-version"
  }
}
