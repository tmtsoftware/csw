/* eslint-disable import/first */
import {TMTAuth} from '../../components/TMTAuth'
jest.mock('keycloak-js')

import KeyCloak from 'keycloak-js'

describe('<TMTAuth />', () => {
  it('should create TMTAuth instance', () => {
    const mockKeycloak = {
      logout: jest.fn(),
      token: 'token string',
      tokenParsed: {name: 'test'},
      realmAccess: { roles: ['test-realm-roles'] },
      resourceAccess: ['test-resource-roles'],
      loadUserInfo: jest.fn(),
      authenticated: false
    }

    const tmtAuth = TMTAuth.from(mockKeycloak)

    expect(tmtAuth.logout).toBe(mockKeycloak.logout)
    expect(tmtAuth.token).toBe(mockKeycloak.token)
    expect(tmtAuth.tokenParsed).toBe(mockKeycloak.tokenParsed)
    expect(tmtAuth.realmAccess).toBe(mockKeycloak.realmAccess)
    expect(tmtAuth.resourceAccess).toBe(mockKeycloak.resourceAccess)
    expect(tmtAuth.loadUserInfo).toBe(mockKeycloak.loadUserInfo)
    expect(tmtAuth.isAuthenticated).toBe(mockKeycloak.authenticated)
    expect(tmtAuth.hasRealmRole).toBe(mockKeycloak.hasRealmRole)
    expect(tmtAuth.hasResourceRole).toBe(mockKeycloak.hasResourceRole)
  })

  it('should authenticate', () => {
    const mockKeycloak = {
      init: jest.fn().mockImplementation(() => {
        return Promise.resolve(true)
      }),
      onTokenExpired: jest.fn(),
      updateToken: jest.fn().mockImplementation(() => {
        return Promise.resolve(true)
      })
    }

    const initMock = jest.spyOn(mockKeycloak, 'init')

    KeyCloak.mockReturnValue(mockKeycloak)

    const {keycloak, authenticated} = TMTAuth.authenticate({
      'realm': 'example',
      'clientId': 'example-app'
    }, 'http://somehost:someport')

    expect(initMock).toHaveBeenCalledWith({onLoad: 'login-required', flow: 'hybrid'})
    expect(keycloak).toBe(mockKeycloak)
    expect(authenticated).toEqual(Promise.resolve(true))
    initMock.mockRestore()
  })
})
