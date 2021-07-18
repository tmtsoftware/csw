package csw.contract.generator

import java.nio.file.{Files, Paths}

import csw.contract.ResourceFetcher
import io.bullet.borer.Encoder

object FilesGenerator extends ContractCodecs {
  val ReadmeName = "README.md"

  def generate(services: Services, outputPath: String): Unit = {
    services.data.foreach { case (serviceName, service) =>
      writeData(s"$outputPath/$serviceName/", "http-contract", service.`http-contract`)
      writeData(s"$outputPath/$serviceName/", "websocket-contract", service.`websocket-contract`)
      writeData(s"$outputPath/$serviceName/", "models", service.models)
      writeReadme(outputPath, serviceName, service.readme)
    }
    generateEntireJson(services, outputPath)
    writeReadme(outputPath, "", Readme(ResourceFetcher.getResourceAsString(ReadmeName)))
  }

  def generateEntireJson(services: Services, outputPath: String): Unit = {
    writeData(s"$outputPath", "allServiceData", services.data)
  }

  def writeReadme(outputPath: String, serviceName: String, readme: Readme): Unit = {
    val destination = Paths.get(s"$outputPath/$serviceName/$ReadmeName")
    Files.write(destination, readme.content.getBytes)
  }

  def writeData[T: Encoder](dir: String, fileName: String, data: T): Unit = {
    Files.createDirectories(Paths.get(dir))
    Files.writeString(Paths.get(dir, s"$fileName.json"), JsonHelper.toJson(data))
  }
}
