package csw.apps.clusterseed.admin.exceptions

/**
 * An Exception representing failure of non existing component name
 *
 * @param componentName component name
 */
//TODO: add doc to explain significance
case class InvalidComponentNameException(componentName: String)
    extends RuntimeException(s"$componentName is not a valid component name")

//TODO: add doc to explain significance
case class UnresolvedAkkaOrHttpLocationException(componentName: String)
    extends RuntimeException(s"Could not resolve $componentName to a valid Akka or Http location")
