export const mockTMTAuth = {
  token: 'some token'
}

export const mockKeycloak = mockTMTAuth
const authenticated = Promise.resolve(true)

export const TMTAuth = {
  resolveAAS: jest.fn().mockImplementation(() => {
    return 'http://mockhost:mockport'
  }),
  from: jest.fn().mockImplementation(() => {
    return mockTMTAuth
  }),
  authenticate: jest.fn().mockImplementation(() => {
    return { mockKeycloak, authenticated }
  })
}
