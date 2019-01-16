import { defaultState } from '../../../components/context/TMTAuthContext'

describe('<TMTAuthContext />', () => {
  it('should have correct default state', () => {
    expect(defaultState.login()).toBe(true)
    expect(defaultState.logout()).toBe(true)
    expect(defaultState.tmtAuth).toBe(null)
  })
})
