import React from 'react'
import Enzyme, { mount } from 'enzyme'
import Adapter from 'enzyme-adapter-react-16'
import { Login } from '../../components/Login'

describe('<Login />', () => {
  Enzyme.configure({ adapter: new Adapter() })

  it('should call login', async () => {
    const props = {
      login: jest.fn()
    }

    const wrapper = await mount(<Login {...props} />)

    wrapper.find('button').simulate('click');

    expect(wrapper.props().login).toHaveBeenCalled()

    wrapper.unmount();
  })
})
