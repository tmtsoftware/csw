import { AASConfig, Config } from '../config/configs'
import KeyCloak from 'keycloak-js'
import { resolveAAS } from './AASResolver'

/**
 * Utility class which talks to keycloak-js adapter and hides keycloak-js from other parts of code.
 */
class TMTAuthStore {
  /**
   * Create instance of TMTAuthStore from keycloak. TMTAuthStore exposes only needed attributes and methods
   * from keycloak instance
   *
   * @param keycloak keycloak instance created after instantiating keycloak using keyclok-js
   * @return TMTAuthStore instance which is created using only needed attributes and methods from keycloak instance
   */
  from = keycloak => {
    this.logout = keycloak.logout
    this.token = () => keycloak.token
    this.tokenParsed = () => keycloak.tokenParsed
    this.realmAccess = () => keycloak.realmAccess // todo: should this be called realmRoles?
    this.resourceAccess = () => keycloak.resourceAccess // todo: should this be called resourceRoles?
    this.loadUserInfo = keycloak.loadUserInfo
    this.isAuthenticated = () => {
      return keycloak.authenticated
    }
    this.hasRealmRole = keycloak.hasRealmRole
    this.hasResourceRole = keycloak.hasResourceRole
    return this
  }

  /**
   * Responsible for instantiating keycloak using provided config and authentication. It also creates hooks for refreshing token when
   * token is expired which silently refresh token resulting seamless user experience once logged in
   *
   * @param config json object which is UI application specific keycloak configuration e.g. realm and clientID.
   * @param url json object which contais AAS url
   * @param redirect boolean which desides instantiation mode for keycloak. e.g. login-required or check-sso.
   * login-required mode redirects user to login screen if not logged in alredy. check-sso only checks if already
   * logged in without redirecting to login screen if not logged in.
   * @return { keycloak, authenticated } json which contains keyclok instance and authenticated which is promise after
   * initializing keycloak
   */
  authenticate = (config, url, redirect) => {
    console.info('instantiating AAS')
    const keycloakConfig = { ...AASConfig, ...config, ...url }
    const keycloak = KeyCloak(keycloakConfig)

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

    const authenticated = keycloak.init({
      onLoad: redirect ? 'login-required' : 'check-sso',
      flow: 'hybrid',
    })
    return { keycloak, authenticated }
  }

  /**
   * Responsible for resolving AAS Server using location service. If not found returns AAS-server-url specified in
   * config
   *
   * @return url string which is AAS server url
   */
  getAASUrl = async () => {
    let url = await resolveAAS()
    return url || Config['AAS-server-url']
  }
}

export const TMTAuth = new TMTAuthStore()
