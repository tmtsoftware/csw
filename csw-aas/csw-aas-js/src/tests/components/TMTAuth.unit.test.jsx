import { TMTAuth } from '../../components/TMTAuth'
import KeyCloak from 'keycloak-js'
import { resolveAAS } from '../../components/AASResolver'

jest.mock('keycloak-js')

jest.mock('../../components/AASResolver')

// DEOPSCSW-630 - Javascript adapter for AAS
describe('<TMTAuth />', () => {
  beforeEach(() => {
    resolveAAS.mockClear()
  })

  it('should create TMTAuth instance', () => {
    const mockKeycloak = {
      logout: jest.fn(),
      token: 'token string',
      tokenParsed: { name: 'test' },
      realmAccess: { roles: ['test-realm-roles'] },
      resourceAccess: ['test-resource-roles'],
      loadUserInfo: jest.fn(),
      authenticated: false,
    }

    const tmtAuth = TMTAuth.from(mockKeycloak)

    expect(tmtAuth.logout).toBe(mockKeycloak.logout)
    expect(tmtAuth.token).toBe(mockKeycloak.token)
    expect(tmtAuth.tokenParsed).toBe(mockKeycloak.tokenParsed)
    expect(tmtAuth.realmAccess).toBe(mockKeycloak.realmAccess)
    expect(tmtAuth.resourceAccess).toBe(mockKeycloak.resourceAccess)
    expect(tmtAuth.loadUserInfo).toBe(mockKeycloak.loadUserInfo)
    expect(tmtAuth.isAuthenticated()).toBe(false)
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
      }),
    }

    const initMock = jest.spyOn(mockKeycloak, 'init')

    KeyCloak.mockReturnValue(mockKeycloak)

    const { keycloak, authenticated } = TMTAuth.authenticate(
      {
        realm: 'example',
        clientId: 'example-app',
      },
      'http://somehost:someport',
      true,
    )

    expect(initMock).toHaveBeenCalledWith({
      onLoad: 'login-required',
      flow: 'hybrid',
    })
    expect(keycloak).toBe(mockKeycloak)
    expect(authenticated).toEqual(Promise.resolve(true))
    initMock.mockRestore()
  })

  it('should getAASUrl from location service', async () => {
    resolveAAS.mockReturnValue(Promise.resolve('http://AAS_IP:AAS_Port/auth'))

    const url = await TMTAuth.getAASUrl()

    expect(resolveAAS).toHaveBeenCalledTimes(1)
    expect(url).toBe('http://AAS_IP:AAS_Port/auth')
  })

  it('should getAASUrl from config', async () => {
    resolveAAS.mockReturnValue(Promise.resolve(null))

    const url = await TMTAuth.getAASUrl()

    expect(resolveAAS).toHaveBeenCalledTimes(1)
    expect(url).toBe('http://localhost:8081/auth')
  })
})
