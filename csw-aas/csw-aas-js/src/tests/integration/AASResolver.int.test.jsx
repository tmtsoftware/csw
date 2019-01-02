import { resolveAAS } from '../../components/AASResolver'

describe('<AASResolver />', () => {
  it('should getAASUrl', async () => {
    const url = await resolveAAS()
    expect(url).not.toContain('localhost')
    expect(url).toMatch(/http:\/\/[0-9]+.[0-9]+.[0-9]+.[0-9]+:[0-9]+\/auth/)
  })
})
