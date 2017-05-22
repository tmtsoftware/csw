package csw.services

import com.persist.JsonOps._

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
 * The package for the logging API.
 */
package object logging {

  /**
   * The type for rich messages.
   * This can be a String or Map[String,String]
   * See the project README file for other options.
   *
   */
  type RichMsg = Any

  /**
   * Marker to indicate no exception is present
   */
  val noException = new Exception("No Exception")

  /**
   * Convert a rich message to a printable string.
   * @param m  the rich message.
   * @return  a string that shows the message contents.
   */
  def richToString(m: RichMsg): String =
    m match {
      case s: String => s
      case x         => Compact(x, safe = true)
    }

  implicit def sourceLocation: () => SourceLocation = macro sourceLocationMacro

  def sourceLocationMacro(c: blackbox.Context): c.Expr[() => SourceLocation] = {
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

    c.Expr[() => SourceLocation](q"() => SourceLocation($file,$packageName,$className,$line)")
  }
}
