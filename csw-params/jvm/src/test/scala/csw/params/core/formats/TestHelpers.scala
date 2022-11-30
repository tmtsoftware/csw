/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.params.core.formats

import csw.params.core.generics.Parameter
import io.bullet.borer.Cbor

object TestHelpers {
  def cborParamEncode(parameter: Parameter[?]): Array[Byte] =
    Cbor.encode(parameter)(ParamCodecs.paramEncExistential).toByteArray

  def cborParamDecode(bytes: Array[Byte]): Parameter[_] =
    Cbor.decode(bytes).to[Parameter[?]](ParamCodecs.paramDecExistential).value
}
