/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.aas.core.deployment
import java.net.URI

import com.typesafe.config.{ConfigException, ConfigFactory}
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{ComponentId, ComponentType, HttpLocation, Metadata}
import csw.prefix.models.{Prefix, Subsystem}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.jdk.CollectionConverters.*

class AuthConfigTest extends AnyFunSuite with Matchers {
  test("should create KeycloakDeployment with auth disabled when authServiceLocation not passed") {
    val config     = ConfigFactory.load()
    val authConfig = AuthConfig(config, None)

    val deployment = authConfig.getDeployment

    deployment.getResourceName shouldBe config.getConfig("auth-config").getString("client-id")
    deployment.getRealm shouldBe config.getConfig("auth-config").getString("realm")
    deployment.getAuthServerBaseUrl shouldBe "http://disabled-auth-service"
    authConfig.disabled shouldBe true
  }

  test("should create KeycloakDeployment with auth enabled when authServiceLocation passed") {
    val config        = ConfigFactory.load()
    val authServerUrl = "http://somehost:someport"
    val httpLocation = HttpLocation(
      HttpConnection(ComponentId(Prefix(Subsystem.CSW, "testComponent"), ComponentType.Service)),
      new URI(authServerUrl),
      Metadata.empty
    )
    val authConfig = AuthConfig(config, authServiceLocation = Some(httpLocation))

    val deployment = authConfig.getDeployment

    deployment.getResourceName shouldBe config.getConfig("auth-config").getString("client-id")
    deployment.getRealm shouldBe config.getConfig("auth-config").getString("realm")
    deployment.getAuthServerBaseUrl shouldBe authServerUrl
    authConfig.disabled shouldBe false

  }

  test("should throw exception if client-id is not present in config") {
    val map: Map[String, String] = Map("auth-config.realm" -> "test")
    val config                   = ConfigFactory.parseMap(map.asJava)

    val authConfig = AuthConfig(config, None)

    intercept[ConfigException.Missing] {
      authConfig.getDeployment
    }
  }
}
