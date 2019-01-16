import { resolveAAS } from '../../components/AASResolver'
import { Config } from '../config/configs'

// DEOPSCSW-630 - Javascript adapter for AAS
describe('<AASResolver />', () => {
  it('should getAASUrl', async () => {
    const testURL = `${Config['location-server-url']}/location/resolve/${
      Config['AAS-server-name']
    }?within=5seconds`

    const url = await resolveAAS(testURL)
    expect(url).not.toContain('localhost')
    expect(url).toMatch(/http:\/\/[0-9]+.[0-9]+.[0-9]+.[0-9]+:[0-9]+\/auth/)
  })
})
