package csw.aas.core.token.claims

/**
 * Contains permissions of the subject
 *
 * @param rsid resource id
 * @param rsname resource name
 * @param scopes permission name
 */
case class Permission(rsid: String, rsname: String, scopes: Set[String] = Set.empty)
