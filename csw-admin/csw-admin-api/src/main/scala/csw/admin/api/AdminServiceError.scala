package csw.admin.api

trait AdminServiceError

object AdminServiceError {
  case class UnresolvedAkkaLocation(componentName: String)
      extends RuntimeException(s"Could not resolve $componentName to a valid Akka location")
      with AdminServiceError
}
