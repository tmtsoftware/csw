/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.contract.generator

import io.bullet.borer.Encoder
import io.bullet.borer.derivation.CompactMapBasedCodecs.deriveEncoder

object ContractCodecs extends ContractCodecs
trait ContractCodecs {
  implicit lazy val endpointEncoder: Encoder[Endpoint]      = deriveEncoder
  implicit lazy val modelTypeEncoder: Encoder[ModelType[_]] = Encoder((w, v) => v.write(w))

  implicit lazy val modelSetEncoder: Encoder[ModelSet] = Encoder[Map[String, ModelType[_]]]
    .contramap(_.modelTypes.map(x => x.name -> x).toMap)

  implicit lazy val readmeEncoder: Encoder[Readme]     = deriveEncoder
  implicit lazy val contractEncoder: Encoder[Contract] = deriveEncoder
  implicit lazy val serviceEncoder: Encoder[Service]   = deriveEncoder
  implicit lazy val servicesEncoder: Encoder[Services] = deriveEncoder
}
