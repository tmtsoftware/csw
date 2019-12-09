package csw.admin.api

class UnresolvedAkkaLocationException(componentName: String)
    extends RuntimeException(s"Could not resolve $componentName to a valid Akka location")
