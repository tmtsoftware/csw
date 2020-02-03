package csw.contract.generator

import java.nio.file.{Files, Paths, StandardCopyOption}

import io.bullet.borer.Encoder

object FilesGenerator extends ContractCodecs {

  def generate(services: Services, outputPath: String, resourcePath: String): Unit = {
    services.data.foreach {
      case (serviceName, service) =>
        writeData(s"$outputPath/$serviceName/", "http-contract", service.`http-contract`)
        writeData(s"$outputPath/$serviceName/", "websocket-contract", service.`websocket-contract`)
        writeData(s"$outputPath/$serviceName/", "models", service.models)
        copyReadme(outputPath, serviceName, resourcePath)
    }
    generateEntireJson(services, outputPath)
    copyReadme(outputPath, "", resourcePath)
  }

  def generateEntireJson(services: Services, outputPath: String): Unit = {
    writeData(s"$outputPath", "allServiceData", services.data)
  }

  def copyReadme(outputPath: String, serviceName: String, resourcePath: String): Unit = {
    val Readme      = "README.md"
    val source      = Paths.get(s"$resourcePath/$serviceName/$Readme")
    val destination = Paths.get(s"$outputPath/$serviceName/$Readme")
    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
  }

  def writeData[T: Encoder](dir: String, fileName: String, data: T): Unit = {
    Files.createDirectories(Paths.get(dir))
    Files.writeString(Paths.get(dir, s"$fileName.json"), JsonHelper.toJson(data))
  }
}
