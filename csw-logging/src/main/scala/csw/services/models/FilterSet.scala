package csw.services.models

import com.typesafe.config.Config
import csw.services.logging.LoggingLevels.Level
import csw.services.logging.{richToString, RichMsg}

import scala.collection.JavaConverters._
import scala.util.Try

case class FilterSet(filters: Map[String, Level]) {
  def check(record: Map[String, RichMsg], level: Level): Boolean = filters.exists {
    case (name, filterLevel) =>
      if (name == richToString(record("cls"))) filterLevel >= level else true
  }

  def add(name: String, level: Level): FilterSet = FilterSet(filters + (name → level))
}

object FilterSet {
  def from(loggingConfig: Config): FilterSet = FilterSet {
    Try {
      loggingConfig
        .getObject("filters")
        .unwrapped()
        .asScala
        .map {
          case (name, filterLevel) ⇒ (name, Level(filterLevel.toString))
        }
        .toMap
    }.getOrElse(Map.empty)
  }
}
