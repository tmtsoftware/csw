package csw.contract

import csw.contract.data.CswData
import csw.contract.generator.FilesGenerator

object Main {
  def main(args: Array[String]): Unit = {
    val DefaultOutputPath   = "csw-contract/target/contracts"
    val DefaultResourcePath = "csw-contract/src/main/resources"
    val outputPath          = if (args.isEmpty) DefaultOutputPath else args(0)
    val resourcesPath       = if (args.isEmpty) DefaultResourcePath else args(1)
    FilesGenerator.generate(CswData.services, outputPath, resourcesPath)
  }
}
