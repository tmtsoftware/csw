package csw.database.client.demo.jooq.dsl_handle
import java.util.Properties

import com.typesafe.config._
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.jooq.{DSLContext, SQLDialect}
import org.jooq.impl.DSL

import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.collection.mutable

object DatabaseService {

  val defaultDSL: DSLContext = createDsl()

  def createDsl(): DSLContext = {
    val config: Config = ConfigFactory
      .load()
      .getConfig("csw-database.postgres")

    val hikariConfig = new HikariConfig(toProperties(config))

    println(s"Hikari Config properties - ${hikariConfig.getDataSourceProperties}")
    DSL.using(new HikariDataSource(hikariConfig), SQLDialect.POSTGRES_10)
  }

  private def toProperties(c: Config): Properties = {
    def toProps(m: mutable.Map[String, ConfigValue]): Properties = {
      val props = new Properties(null)
      m.foreach {
        case (k, cv) =>
          val v =
            if (cv.valueType() == ConfigValueType.OBJECT) toProps(cv.asInstanceOf[ConfigObject].asScala)
            else if (cv.unwrapped eq null) null
            else cv.unwrapped.toString
          if (v ne null) props.put(k, v)
      }
      props
    }
    toProps(c.root.asScala)
  }
}
