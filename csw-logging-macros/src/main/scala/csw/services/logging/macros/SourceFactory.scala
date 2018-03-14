package csw.services.logging.macros

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

trait SourceFactory {
  def get(): SourceLocation
}

object SourceFactory {
  implicit def factory: SourceFactory = macro sourceLocationMacro

  def from(f: () ⇒ SourceLocation): SourceFactory = () => f()

  def from(cls: Class[_]): SourceFactory = from(() ⇒ SourceLocation("", "", cls.getName, -1))

  def sourceLocationMacro(c: blackbox.Context): c.Expr[SourceFactory] = {
    import c.universe._

    val p    = c.macroApplication.pos
    val file = p.source.file.name
    val line = p.line

    def allOwners(s: c.Symbol): Seq[c.Symbol] =
      if (s == `NoSymbol`) {
        Seq()
      } else {
        s +: allOwners(s.owner)
      }
    val owners = allOwners(c.internal.enclosingOwner)

    val className = owners
      .filter(s => s.toString.startsWith("class") || s.toString.startsWith("object"))
      .map(s => s.asClass.name.toString)
      .reverse
      .mkString("$")
    val packageName = owners
      .filter(_.isPackage)
      .map(_.name.toString())
      .filter(_ != "<root>")
      .reverse
      .mkString(".")

    c.Expr[SourceFactory](
      q"csw.services.logging.macros.SourceFactory.from(() => csw.services.logging.macros.SourceLocation($file,$packageName,$className,$line))"
    )
  }
}
