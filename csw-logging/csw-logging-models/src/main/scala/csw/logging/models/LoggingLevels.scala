package csw.logging.models

import csw.logging.models.codecs.LoggingSerializable
import enumeratum._

import scala.collection.immutable.IndexedSeq

/**
 * A logging level.
 */
sealed abstract class Level(override val entryName: String, val pos: Int)
    extends EnumEntry
    with LoggingSerializable
    with Ordered[Level] {

  /**
   * A level name.
   */
  def name: String = entryName

  /**
   * Compares levels
   *
   * @param that the other level
   * @return `x` where:
   *         - `x < 0` when `this < that`
   *         - `x == 0` when `this == that`
   *         - `x > 0` when  `this > that`
   */
  def compare(that: Level): Int = pos - that.pos
}

/**
 * Companion object for the level trait.
 */
object Level extends Enum[Level] {

  override def values: IndexedSeq[Level] = findValues

  def stringify(): String = values.mkString(",")

  /**
   * Level constructor.
   *
   * @param name a level name. Case is ignored.
   * @return the corresponding Level if there is one for that name. Otherwise WARN.
   */
  def apply(name: String): Level = Level.withNameInsensitiveOption(name).getOrElse(WARN)

  /**
   * Checks if a level name exists.
   *
   * @param name the level name.
   * @return true if a level with that name exists.
   */
  def hasLevel(name: String): Boolean = namesToValuesMap.get(name.toUpperCase).isDefined

  /**
   * The TRACE logging level.
   */
  case object TRACE extends Level("TRACE", 0)

  /**
   * The DEBUG logging level.
   */
  case object DEBUG extends Level("DEBUG", 1)

  /**
   * The INFO logging level.
   */
  case object INFO extends Level("INFO", 2)

  /**
   * The WARN logging level.
   */
  case object WARN extends Level("WARN", 3)

  /**
   * The ERROR logging level.
   */
  case object ERROR extends Level("ERROR", 4)

  /**
   * The FATAL logging level.
   */
  case object FATAL extends Level("FATAL", 5)
}

/**
 * Current and default logging levels.
 *
 * @param current the current logging level.
 * @param default the default logging level.
 */
case class Levels(current: Level, default: Level)
