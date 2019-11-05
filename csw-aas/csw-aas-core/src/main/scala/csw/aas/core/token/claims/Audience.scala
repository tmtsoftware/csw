package csw.aas.core.token.claims

/**
 * Contains the audience of access token
 */
case class Audience(value: Seq[String] = Seq.empty)

object Audience {

  /**
   * Returns an instance of [[csw.aas.core.token.claims.Audience]] with no values
   */
  val empty: Audience = Audience()

  def apply(aud: String): Audience = Audience(Seq(aud))
}
