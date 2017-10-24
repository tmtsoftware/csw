package csw.services.logging.internal

import csw.messages.TMTSerializable

object LoggingLevels {

  private[this] val levels         = Seq(TRACE, DEBUG, INFO, WARN, ERROR, FATAL)
  private[this] val nameToLevelMap = levels.map(level => (level.name, level)).toMap

  def stringify(): String = nameToLevelMap.keySet.mkString(",")

  /**
   * Companion object for the level trait.
   */
  object Level {

    /**
     *  Level constructor.
     * @param name a level name. Case is ignored.
     * @return the corresponding Level if there is one for that name. Otherwise WARN.
     */
    def apply(name: String): Level = nameToLevelMap.getOrElse(name.toUpperCase(), WARN)

    /**
     * Checks if a level name exists.
     * @param name the level name.
     * @return  true if a level with that name exists.
     */
    def hasLevel(name: String): Boolean = nameToLevelMap.get(name.toUpperCase).isDefined
  }

  /**
   * A logging level.
   */
  sealed trait Level extends Ordered[Level] with TMTSerializable {
    private[logging] val pos: Int

    /**
     * A level name.
     */
    val name: String

    /**
     * Compares levels
     * @param that the other level
     * @return `x` where:
     *         - `x < 0` when `this < that`
     *         - `x == 0` when `this == that`
     *         - `x > 0` when  `this > that`
     */
    def compare(that: Level): Int = pos - that.pos
  }

  /**
   * The TRACE logging level.
   */
  case object TRACE extends Level {
    private[logging] val pos = 0

    /**
     * Level name "TRACE".
     */
    val name = "TRACE"

  }

  /**
   * The DEBUG logging level.
   */
  case object DEBUG extends Level {
    private[logging] val pos = 1

    /**
     * Level name "DEBUG".
     */
    val name = "DEBUG"
  }

  /**
   * The INFO logging level.
   */
  case object INFO extends Level {
    private[logging] val pos = 2

    /**
     * Level name "INFO".
     */
    val name = "INFO"
  }

  /**
   * The WARN logging level.
   */
  case object WARN extends Level {
    private[logging] val pos = 3

    /**
     * Level name "WARN".
     */
    val name = "WARN"
  }

  /**
   * The ERROR logging level.
   */
  case object ERROR extends Level {
    private[logging] val pos = 4

    /**
     * Level name "Error"
     */
    val name = "ERROR"
  }

  /**
   * The FATAL logging level.
   */
  case object FATAL extends Level {
    private[logging] val pos = 5

    /**
     * Level name "FATAL".
     */
    val name = "FATAL"
  }

  /**
   * Current and default logging levels.
   * @param current the current logging level.
   * @param default the default logging level.
   */
  case class Levels(current: Level, default: Level)

}
