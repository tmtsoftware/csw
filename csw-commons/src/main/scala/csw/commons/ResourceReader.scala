/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.commons

import java.io.File
import java.nio.file.{Files, Path}
import scala.io.Source

object ResourceReader {
  type TempFilePath = Path
  type FileContent  = String

  def copyToTmp(fileName: String, suffix: String = ".tmp"): TempFilePath = {
    val resourceStream = getClass.getResourceAsStream(fileName)
    try {
      val tempFile = File.createTempFile(String.valueOf(resourceStream.hashCode), suffix)
      tempFile.deleteOnExit()
      Files.write(tempFile.toPath, resourceStream.readAllBytes())
    }
    finally {
      resourceStream.close()
    }
  }

  def readAndCopyToTmp(fileName: String): (TempFilePath, FileContent) = {
    val tempPath = copyToTmp(fileName)
    val source   = Source.fromFile(tempPath.toFile)
    try {
      (tempPath, source.mkString)
    }
    finally {
      source.close()
    }
  }
}
