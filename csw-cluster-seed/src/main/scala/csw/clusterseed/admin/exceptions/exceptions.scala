package csw.clusterseed.admin.exceptions

case class InvalidComponentNameException(componentName: String)
    extends RuntimeException(s"$componentName is not a valid component name")

case class UnresolvedAkkaOrHttpLocationException(componentName: String)
    extends RuntimeException(s"Could not resolve $componentName to a valid Akka or Http location")
