/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.aas.core.token

import csw.aas.core.TokenVerifier
import msocket.security.api.TokenValidator
import msocket.security.models.AccessToken

import scala.concurrent.{ExecutionContext, Future}

class TokenFactory(tokenVerifier: TokenVerifier)(implicit ec: ExecutionContext) extends TokenValidator {

  /**
   * It will validate the token string for signature and expiry and then decode it into
   * [[msocket.security.models.AccessToken]]
   *
   * @param token Access token string
   */
  override def validate(token: String): Future[AccessToken] =
    tokenVerifier.verifyAndDecode(token).map {
      case Left(failure)      => throw failure
      case Right(accessToken) => accessToken
    }
}
