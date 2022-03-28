/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.aas.http

import msocket.security.models.AccessToken

import scala.concurrent.Future
import scala.util.Try

/**
 * A synchronous authorization policy is a way to filter incoming HTTP requests based on rules
 */
trait SyncAuthorizationPolicy extends msocket.security.api.AuthorizationPolicy {

  /**
   * Implement this method to create a synchronous AuthorizationPolicy
   */
  protected def syncAuthorize(accessToken: AccessToken): Boolean

  final override def authorize(accessToken: AccessToken): Future[Boolean] = Future.fromTry(Try(syncAuthorize(accessToken)))
}

/**
 * An authorization policy is a way to provide filter incoming HTTP requests based on standard rules.
 */
object AuthorizationPolicy {

  /**
   * This policy filters requests based on Realm Role.
   *
   * A Realm Role is global role within a realm and is applicable for all clients within realm.
   *
   * @param name Name of role
   */
  final case class RealmRolePolicy(name: String) extends SyncAuthorizationPolicy {
    override protected def syncAuthorize(accessToken: AccessToken): Boolean = accessToken.hasRealmRole(name)
  }

  /**
   * Allows custom request filtering based on access token properties.
   *
   * @param predicate Filter
   */
  final case class CustomPolicy(predicate: AccessToken => Boolean) extends SyncAuthorizationPolicy {
    override protected def syncAuthorize(accessToken: AccessToken): Boolean = predicate(accessToken)
  }

  /**
   * Allows custom request filtering based on access token properties.
   *
   * @param predicate Async filter
   */
  final case class CustomPolicyAsync(predicate: AccessToken => Future[Boolean]) extends msocket.security.api.AuthorizationPolicy {
    override def authorize(accessToken: AccessToken): Future[Boolean] = predicate(accessToken)
  }

  /**
   * Use this when you only need authentication but not authorization
   */
  case object EmptyPolicy extends SyncAuthorizationPolicy {
    override protected def syncAuthorize(accessToken: AccessToken): Boolean = true
  }
}
