package csw.services.alarm.client.internal.configparser

import java.io.{ByteArrayInputStream, File, IOException, InputStream}
import java.net.URI

import com.fasterxml.jackson.databind.JsonNode
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.core.load.configuration.LoadingConfiguration
import com.github.fge.jsonschema.core.load.download.URIDownloader
import com.github.fge.jsonschema.core.report.ProcessingMessage
import com.github.fge.jsonschema.main.{JsonSchema, JsonSchemaFactory}
import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions}
import csw.services.alarm.client.internal.configparser.ValidationResult.{Failure, Success}

import scala.collection.JavaConverters.{asScalaIteratorConverter, iterableAsScalaIterableConverter}

/**
 * Uses json-schema to validate the Alarm Service Config File (ASCF), which is used to
 * import alarm data into the database.
 */
object AscfValidator {
  private val loadingCfg        = LoadingConfiguration.newBuilder.addScheme("config", ConfigDownloader).freeze
  private val jsonSchemaFactory = JsonSchemaFactory.newBuilder.setLoadingConfiguration(loadingCfg).freeze

  /**
   * Returns a string with the contents of the given config, converted to JSON.
   *
   * @param config the config to convert
   * @return the config contents in JSON format
   */
  private def toJson(config: Config): String = config.root.render(ConfigRenderOptions.concise())

  // Adds a custom URI scheme, so that config:/... loads the config file as a resource
  // and converts it to JSON. In this way you can use "$ref": "config:/myfile.conf"
  // to refer to external JSON schemas in HOCON format.
  private case object ConfigDownloader extends URIDownloader {
    override def fetch(uri: URI): InputStream = {
      val config = ConfigFactory.parseResources(uri.getPath.substring(1))
      if (config == null) throw new IOException(s"Resource not found: ${uri.getPath}")
      new ByteArrayInputStream(toJson(config).getBytes)
    }
  }

  /**
   * Validates the given input config using the given schema config.
   *
   * @param inputConfig   the config to be validated against the schema
   * @param schemaConfig  a config using the JSON schema syntax (but may be simplified to HOCON format)
   * @return a list of problems, if any were found
   */
  def validate(inputConfig: Config, schemaConfig: Config): ValidationResult = {
    val jsonSchema = JsonLoader.fromString(toJson(schemaConfig))
    val schema     = jsonSchemaFactory.getJsonSchema(jsonSchema)
    val jsonInput  = JsonLoader.fromString(toJson(inputConfig))
    validate(schema, jsonInput, inputConfig.origin().filename())
  }

  // Runs the validation and handles any internal exceptions
  // 'source' is the name of the input file for use in error messages.
  private def validate(schema: JsonSchema, jsonInput: JsonNode, source: String): ValidationResult = {
    try {
      val report = schema.validate(jsonInput, true)

      if (report.isSuccess) Success
      else Failure(report.asScala.map(formatMsg(_, source)).toList)
    } catch {
      case e: Exception =>
        e.printStackTrace()
        Failure(List(s"fatal: ${e.toString}"))
    }
  }

  // Formats the error message for display to user.
  // 'source' is the name of the original input file.
  private def formatMsg(msg: ProcessingMessage, source: String): String = {
    val file = new File(source).getPath

    // try to get a nicely formatted error message that includes the necessary info
    val json          = msg.asJson()
    val pointer       = json.get("instance").get("pointer").asText()
    val loc           = if (pointer.isEmpty) s"$file" else s"$file, at path: $pointer"
    val schemaUri     = json.get("schema").get("loadingURI").asText()
    val schemaPointer = json.get("schema").get("pointer").asText()
    val schemaStr     = if (schemaUri == "#") "" else s" (schema: $schemaUri:$schemaPointer)"

    // try to get additional messages from the reports section
    val reports = json.get("reports")
    val messages =
      if (reports == null) ""
      else {
        val msgElems = (for (r <- reports.asScala) yield r.elements().asScala.toList).flatten
        val msgTexts = for (e <- msgElems) yield e.get("message").asText()
        "\n" + msgTexts.mkString("\n")
      }

    s"$loc: ${msg.getLogLevel.toString}: ${msg.getMessage}$schemaStr$messages"
  }

}
