/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.contract

import csw.contract.data.CswData
import csw.contract.generator.FilesGenerator

object Main {
  def main(args: Array[String]): Unit = {
    val DefaultOutputPath = "csw-contract/target/contracts"
    val outputPath        = if (args.isEmpty) DefaultOutputPath else args(0)
    FilesGenerator.generate(CswData.services, outputPath)
  }
}
