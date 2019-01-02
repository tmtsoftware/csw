import fetch from 'isomorphic-fetch'
import { resolveAAS } from '../../components/AASResolver'

jest.mock('isomorphic-fetch')

jest.mock('keycloak-js')

describe('<AASResolver />', () => {
  it('should resolveAAS', async () => {
    const mockResponse = {
      status: 200,
      json: jest.fn().mockImplementation(() => {
        return { uri: 'http://somehost:someport' }
      }),
    }
    fetch.mockReturnValue(Promise.resolve(mockResponse))

    const url = await resolveAAS()

    expect(url).toBe('http://somehost:someport')
  })
})
