package csw.command.client.cbor

import csw.command.client.models.framework.LocationServiceUsage
import csw.params.core.formats.CborHelpers
import io.bullet.borer.Codec

object CommandCborSupport {
  implicit lazy val locationServiceUsage: Codec[LocationServiceUsage] = CborHelpers.enumCodec[LocationServiceUsage]
}
