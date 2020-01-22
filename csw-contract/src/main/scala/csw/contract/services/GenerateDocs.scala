package csw.contract.services

import java.nio.file.{Files, Paths}

import csw.contract.services.codecs.ContractCodecs
import csw.contract.services.data.ServiceData
import io.bullet.borer.Json

object GenerateDocs extends ContractCodecs {
  val DefaultOutputPath = "csw-contract/target/output"

  def main(args: Array[String]): Unit = {
    val outputPath = if (args.isEmpty) DefaultOutputPath else args(0)

    ServiceData.data.services.foreach {
      case (serviceName, service) =>
        service.endpoints.foreach {
          case (endpointName, endpoint) =>
            write(s"$outputPath/$serviceName/endpoints", endpointName, Json.encode(endpoint).toUtf8String)
        }
        service.models.foreach {
          case (modelName, model) =>
            write(s"$outputPath/$serviceName/models", modelName, Json.encode(model.models).toUtf8String)
        }
    }
  }

  def write(dir: String, fileName: String, jsonData: String): Unit = {
    Files.createDirectories(Paths.get(dir))
    Files.writeString(Paths.get(dir, fileName), jsonData)
  }
}
