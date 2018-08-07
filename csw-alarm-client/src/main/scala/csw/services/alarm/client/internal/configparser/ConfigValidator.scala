package csw.services.alarm.client.internal.configparser

import java.io.{ByteArrayInputStream, IOException, InputStream}
import java.net.URI

import com.fasterxml.jackson.databind.JsonNode
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.core.load.configuration.LoadingConfiguration
import com.github.fge.jsonschema.core.load.download.URIDownloader
import com.github.fge.jsonschema.core.report.ProcessingMessage
import com.github.fge.jsonschema.main.{JsonSchema, JsonSchemaFactory}
import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions}
import csw.services.alarm.api.internal.ValidationResult
import csw.services.alarm.api.internal.ValidationResult.{Failure, Success}

import scala.collection.JavaConverters.{asScalaIteratorConverter, iterableAsScalaIterableConverter}

/**
 * Uses json-schema to validate the Config File
 */
object ConfigValidator {
  private val loadingCfg        = LoadingConfiguration.newBuilder.addScheme("config", ConfigDownloader).freeze
  private val jsonSchemaFactory = JsonSchemaFactory.newBuilder.setLoadingConfiguration(loadingCfg).freeze

  /**
   * Returns a string with the contents of the given config, converted to JSON.
   *
   * @param config the config to convert
   * @return the config contents in JSON format
   */
  private def conciseJsonFrom(config: Config) = config.root.render(ConfigRenderOptions.concise())
  private def jsonNodeFrom(config: Config)    = JsonLoader.fromString(conciseJsonFrom(config))

  // Adds a custom URI scheme, so that config:/... loads the config file as a resource
  // and converts it to JSON. In this way you can use "$ref": "config:/myfile.conf"
  // to refer to external JSON schemas in HOCON format.
  private case object ConfigDownloader extends URIDownloader {
    override def fetch(uri: URI): InputStream = {
      val config = ConfigFactory.parseResources(uri.getPath.substring(1))
      if (config == null) throw new IOException(s"Resource not found: ${uri.getPath}")
      new ByteArrayInputStream(conciseJsonFrom(config).getBytes)
    }
  }

  /**
   * Validates the given input config using the given schema config.
   *
   * @param inputConfig   the config to be validated against the schema
   * @param schemaConfig  a config using the JSON schema syntax (but may be simplified to HOCON format)
   * @return validation status either Success or Failure with reasons
   */
  def validate(inputConfig: Config, schemaConfig: Config): ValidationResult = {
    val jsonSchema = jsonSchemaFactory.getJsonSchema(jsonNodeFrom(schemaConfig))
    val jsonInput  = jsonNodeFrom(inputConfig)
    val fileName   = inputConfig.origin().filename()

    validate(jsonSchema, jsonInput, fileName)
  }

  // Runs the validation and handles any internal exceptions
  // 'source' is the name of the input file for use in error messages.
  private def validate(jsonSchema: JsonSchema, jsonInput: JsonNode, source: String): ValidationResult = {
    try {
      val processingReport = jsonSchema.validate(jsonInput, true)
      if (processingReport.isSuccess) Success
      else Failure(processingReport.asScala.map(formatMsg(_, source)).toList)
    } catch {
      case e: Exception =>
        e.printStackTrace()
        Failure(List(s"fatal: ${e.toString}"))
    }
  }

  // Formats the error message for display to user.
  // 'source' is the name of the original input file.
  private def formatMsg(msg: ProcessingMessage, source: String): String = {
    // try to get a nicely formatted error message that includes the necessary info
    val json             = msg.asJson()
    val loc              = extractErrorLocation(json, source)
    val schemaStr        = extractJsonSchemaStr(json)
    val messages: String = extractAdditionalMessages(json)

    s"$loc: ${msg.getLogLevel}: ${msg.getMessage}$schemaStr$messages"
  }

  private def extractErrorLocation(json: JsonNode, source: String) = {
    val pointer = json.get("instance").get("pointer").asText()
    if (pointer.isEmpty) s"$source" else s"$source, at path: $pointer"
  }

  private def extractJsonSchemaStr(json: JsonNode) = {
    val schemaUri = json.get("schema").get("loadingURI").asText()

    val schemaPointer = json.get("schema").get("pointer").asText()
    if (schemaUri == "#") "" else s" (schema: $schemaUri:$schemaPointer)"
  }

  // try to get additional messages from the reports section
  private def extractAdditionalMessages(json: JsonNode) =
    Option(json.get("reports"))
      .map { reports ⇒
        reports.asScala
          .flatMap(_.elements().asScala)
          .map(_.get("message").asText())
          .mkString("\n", "\n", "\n")
      }
      .getOrElse("")

}
