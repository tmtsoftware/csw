/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.config.server.commons

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

// DEOPSCSW-47: Unique name for configuration file
// DEOPSCSW-135: Validation of suffix for active and sha files
class PathValidatorTest extends AnyFunSuite with Matchers {

  test("should return false for invalid path | DEOPSCSW-47, DEOPSCSW-135") {

    val paths = List(
      "/invalidpath!/sample.txt",
      "/invalidpath#/sample.txt",
      "/invalidpath$/sample.txt",
      "/invalidpath/%sample.txt",
      "/invalidpath/&sample.txt",
      "/invalidpath/sa'mple.txt",
      "/invalidpath/samp@le.txt",
      "/invalidpath/samp`le.txt",
      "/invalid+path/sample.txt",
      "/invalid,path/sample.txt",
      "/invalidpath;/sample.txt",
      "/invalidpath/sam=ple.txt",
      "/invalid path/sample.txt",
      "/invalidpath/<sample.txt",
      "/invalidpath/sample>.txt"
    )

    paths.foreach { path => PathValidator.isValid(path) shouldBe false }
  }

  test("should return true for valid file path | DEOPSCSW-47, DEOPSCSW-135") {
    PathValidator.isValid("/validpath/sample.txt") shouldBe true
  }
}
