import {TMTAuth} from '../../components/TMTAuth'

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
    };

    const tmtAuth = TMTAuth.from(mockKeycloak);

    expect(tmtAuth.logout).toBe(mockKeycloak.logout);
    expect(tmtAuth.token).toBe(mockKeycloak.token);
    expect(tmtAuth.tokenParsed).toBe(mockKeycloak.tokenParsed);
    expect(tmtAuth.realmAccess).toBe(mockKeycloak.realmAccess);
    expect(tmtAuth.resourceAccess).toBe(mockKeycloak.resourceAccess);
    expect(tmtAuth.loadUserInfo).toBe(mockKeycloak.loadUserInfo);
    expect(tmtAuth.isAuthenticated).toBe(mockKeycloak.authenticated)
  })
});
