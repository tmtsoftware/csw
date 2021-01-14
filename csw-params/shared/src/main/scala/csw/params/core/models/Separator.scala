package csw.params.core.models

object Separator {
  val Hyphen: String = "-"

  def hyphenate(ins: String*): String = ins.mkString(Hyphen)
}
