package csw.contract.services

import java.io.{File, PrintWriter}

import com.typesafe.config.ConfigFactory
import play.api.libs.json
import play.api.libs.json.JsObject

object Main extends App {

  val config = ConfigFactory.load()
  val outputPath = config.getString("csw-contract.outputPath")
  private val locationRegister: EndpointJson = EndpointJson(s"$outputPath/location/register.json", Location.registerSamples)
  private val locationUnregister: EndpointJson = EndpointJson(s"$outputPath/location/unregister.json", Location.unRegisterSamples)

  private val endpointJsons = List(locationRegister, locationUnregister)

  endpointJsons.foreach(write)

  def write(endpointJson: EndpointJson): Unit = {
    val printWriter = new PrintWriter(endpointJson.filePath)
    printWriter.write(json.Json.prettyPrint(endpointJson.samples))
    printWriter.close()
  }
}

case class EndpointJson(filePath: String, samples: JsObject)