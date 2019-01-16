import React from 'react'
import { Logout } from '../../components/Logout'
import Enzyme, { mount } from 'enzyme'
import Adapter from 'enzyme-adapter-react-16'

describe('<Logout />', () => {
  Enzyme.configure({ adapter: new Adapter() })

  it('should call logout', async () => {
    const props = {
      logout: jest.fn(),
    }

    const wrapper = await mount(<Logout {...props} />)

    wrapper.find('button').simulate('click');

    expect(wrapper.props().logout).toHaveBeenCalled()

    wrapper.unmount();
  })
})
