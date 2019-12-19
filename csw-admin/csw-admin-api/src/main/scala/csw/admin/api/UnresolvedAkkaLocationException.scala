package csw.admin.api

import csw.prefix.models.Prefix

class UnresolvedAkkaLocationException(prefix: Prefix)
    extends RuntimeException(s"Could not resolve ${prefix.value} to a valid Akka location")
