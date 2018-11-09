package csw.auth

private[auth] object Authorization {
  def hasPermission(accessToken: AccessToken, permission: String): Boolean = {
    accessToken.permissions.contains(permission)
  }

  def hasRole(accessToken: AccessToken, role: String): Boolean = {
    accessToken.roles.contains(role)
  }
}
