package csw.contract.services

import java.io.{File, PrintWriter}

import com.typesafe.config.ConfigFactory
import csw.contract.services.codecs.ContractCodecs
import csw.contract.services.data.ServiceData
import io.bullet.borer.Json
import play.api.libs.json

object GenerateDocs extends ContractCodecs {
  private val config     = ConfigFactory.load()
  private val outputPath = config.getString("csw-contract.outputPath")

  def main(args: Array[String]): Unit = {
    ServiceData.data.services.foreach {
      case (serviceName, service) => {
        service.endpoints.foreach {
          case (endpointName, endpoint) => {
            write(
              s"$outputPath/$serviceName/endpoints/$endpointName.json",
              Json.encode(Map("requests" -> endpoint.requests, "responses" -> endpoint.responses)).toUtf8String
            )
          }
        }
        service.models.foreach {
          case (modelName, model) => {
            write(s"$outputPath/$serviceName/models/$modelName.json", Json.encode(model.models).toUtf8String)
          }
        }
      }
    }
  }

  def write(filePath: String, jsonString: String): Unit = {
    val file = new File(filePath)
    file.getParentFile.mkdirs
    val printWriter = new PrintWriter(file)
    printWriter.write(json.Json.prettyPrint(json.Json.parse(jsonString)))
    printWriter.close()
  }
}
