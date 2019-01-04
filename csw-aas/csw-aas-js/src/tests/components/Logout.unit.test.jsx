import React from 'react'
import { Logout } from '../../components/Logout'
import Enzyme, { mount } from 'enzyme'
import Adapter from 'enzyme-adapter-react-16'

describe('<Logout />', () => {
  Enzyme.configure({ adapter: new Adapter() })

  it('should call logout', async () => {
    const props = {
      isAuthenticated: true,
      logout: jest.fn(),
    }

    const wrapper = await mount(<Logout {...props} />)

    expect(wrapper.props().logout).toHaveBeenCalled()
  })

  it('should not call logout if un-authenticated', async () => {
    const props = {
      isAuthenticated: false,
      logout: jest.fn(),
    }

    const wrapper = await mount(<Logout {...props} />)

    expect(wrapper.props().logout).not.toHaveBeenCalled()
  })
})
