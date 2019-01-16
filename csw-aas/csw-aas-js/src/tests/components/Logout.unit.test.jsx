import React from 'react'
import { Logout } from '../../components/Logout'
import Enzyme, { mount } from 'enzyme'
import Adapter from 'enzyme-adapter-react-16'
import renderer from 'react-test-renderer'

// DEOPSCSW-630 - Javascript adapter for AAS
// DEOPSCSW-631 - React layer for javascript adapter for AAS
describe('<Logout />', () => {
  Enzyme.configure({ adapter: new Adapter() })

  it('should call logout', async () => {
    const props = {
      logout: jest.fn(),
    }

    const wrapper = await mount(<Logout {...props} />)

    wrapper.find('button').simulate('click')

    expect(wrapper.props().logout).toHaveBeenCalled()

    wrapper.unmount()
  })

  it('should render logout', () => {
    const props = {
      logout: jest.fn(),
    }

    const logout = renderer.create(<Logout {...props} />).toJSON()
    expect(logout).toMatchSnapshot()
  })
})
