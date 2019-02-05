export const mockAuth = {
  token: 'some token',
}

export const mockKeycloak = mockAuth
const authenticated = Promise.resolve(true)

export const Auth = {
  getAASUrl: jest.fn().mockImplementation(() => {
    return 'http://mockhost:mockport'
  }),
  from: jest.fn().mockImplementation(() => {
    return mockAuth
  }),
  authenticate: jest.fn().mockImplementation(() => {
    return { mockKeycloak, authenticated }
  }),
}
