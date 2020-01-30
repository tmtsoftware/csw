package csw.contract.generator

import java.nio.file.{Files, Paths}

import io.bullet.borer.Encoder

object FilesGenerator extends ContractCodecs {

  def generate(services: Services, outputPath: String): Unit = {
    services.data.foreach {
      case (serviceName, service) =>
        writeData(s"$outputPath/$serviceName/", "http-contract", service.`http-contract`)
        writeData(s"$outputPath/$serviceName/", "websocket-contract", service.`websocket-contract`)
        writeData(s"$outputPath/$serviceName/", "models", service.models)
    }
    generateEntireJson(services, outputPath)
  }

  def generateEntireJson(services: Services, outputPath: String): Unit = {
    writeData(s"$outputPath", "allServiceData", services.data)
  }

  def writeData[T: Encoder](dir: String, fileName: String, data: T): Unit = {
    Files.createDirectories(Paths.get(dir))
    Files.writeString(Paths.get(dir, s"$fileName.json"), JsonHelper.toJson(data))
  }
}
