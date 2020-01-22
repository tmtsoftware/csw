package csw.aas.core.token.claims

import csw.prefix.models.Subsystem

/**
 * Contains subsystems of a user or client
 */
case class TokenSubsystems(values: Set[Subsystem] = Set.empty)

object TokenSubsystems {
  val empty: TokenSubsystems = TokenSubsystems()
}
