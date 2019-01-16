import { AASConfig, Config } from '../config/configs'
import KeyCloak from 'keycloak-js'
import { resolveAAS } from './AASResolver'

class TMTAuthStore {

  from = keycloak => {
    this.logout = keycloak.logout
    this.token = keycloak.token
    this.tokenParsed = keycloak.tokenParsed
    this.realmAccess = keycloak.realmAccess //todo: should this be called realmRoles?
    this.resourceAccess = keycloak.resourceAccess  //todo: should this be called resourceRoles?
    this.loadUserInfo = keycloak.loadUserInfo
    this.isAuthenticated = () => { return keycloak.authenticated }
    this.hasRealmRole = keycloak.hasRealmRole
    this.hasResourceRole = keycloak.hasResourceRole
    return this
  }

  authenticate = (config, url, redirect) => {
    console.info('instantiating AAS')
    const keycloakConfig = { ...AASConfig, ...config, ...url }
    const keycloak = KeyCloak(keycloakConfig)
    if (redirect) {
      keycloak.onTokenExpired = () => {
        keycloak
          .updateToken(0)
          .success(function() {
            console.info('token refreshed successfully')
          })
          .error(function() {
            console.error(
              'Failed to refresh the token, or the session has expired',
            )
          })
      }
    }

    const authenticated = keycloak.init({
      onLoad: redirect ? 'login-required' : 'check-sso',
      flow: 'hybrid',
    })
    return { keycloak, authenticated }
  }

  getAASUrl = async () => {
    let url = await resolveAAS()
    return url || Config['AAS-server-url']
  }
}

export const TMTAuth = new TMTAuthStore()
