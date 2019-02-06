import { Auth } from '../../components/Auth'
import KeyCloak from 'keycloak-js'
import { resolveAAS } from '../../components/AASResolver'

jest.mock('keycloak-js')

jest.mock('../../components/AASResolver')

// DEOPSCSW-630 - Javascript adapter for AAS
describe('<Auth />', () => {
  beforeEach(() => {
    resolveAAS.mockClear()
  })

  it('should create Auth instance', () => {
    const mockKeycloak = {
      logout: jest.fn(),
      token: 'token string',
      tokenParsed: { name: 'test' },
      realmAccess: { roles: ['test-realm-roles'] },
      resourceAccess: ['test-resource-roles'],
      loadUserProfile: jest.fn(),
      authenticated: false,
    }

    const auth = Auth.from(mockKeycloak)

    expect(auth.logout).toBe(mockKeycloak.logout)
    expect(auth.token()).toBe(mockKeycloak.token)
    expect(auth.tokenParsed()).toBe(mockKeycloak.tokenParsed)
    expect(auth.realmAccess()).toBe(mockKeycloak.realmAccess)
    expect(auth.resourceAccess()).toBe(mockKeycloak.resourceAccess)
    expect(auth.loadUserProfile).toBe(mockKeycloak.loadUserProfile)
    expect(auth.isAuthenticated()).toBe(false)
    expect(auth.hasRealmRole).toBe(mockKeycloak.hasRealmRole)
    expect(auth.hasResourceRole).toBe(mockKeycloak.hasResourceRole)
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

    const { keycloak, authenticated } = Auth.authenticate(
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

    const url = await Auth.getAASUrl()

    expect(resolveAAS).toHaveBeenCalledTimes(1)
    expect(url).toBe('http://AAS_IP:AAS_Port/auth')
  })

  it('should getAASUrl from config', async () => {
    resolveAAS.mockReturnValue(Promise.resolve(null))

    const url = await Auth.getAASUrl()

    expect(resolveAAS).toHaveBeenCalledTimes(1)
    expect(url).toBe('http://localhost:8081/auth')
  })
})
