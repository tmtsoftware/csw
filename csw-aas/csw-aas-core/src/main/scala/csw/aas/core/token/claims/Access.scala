package csw.aas.core.token.claims

/**
 * Contains roles of a user or client
 */
case class Access(roles: Set[String] = Set.empty)

object Access {

  /**
   * Returns an instance of [[csw.aas.core.token.claims.Access]] which has no roles
   */
  val empty: Access = Access()
}
