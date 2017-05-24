package csw.admin

/**
 * An Exception representing failure of non existing component name
 *
 * @param componentName component name
 */
case class InvalidComponentNameException(componentName: String)
    extends RuntimeException(s"$componentName is not a valid component name")
