import { AuthContextDefaultState } from '../../../components/context/AuthContext'

describe('<AuthContext />', () => {
  it('should have correct default state', () => {
    expect(AuthContextDefaultState.login()).toBe(true)
    expect(AuthContextDefaultState.logout()).toBe(true)
    expect(AuthContextDefaultState.auth).toBe(undefined)
  })
})
