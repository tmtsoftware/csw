import React from 'react'
import Enzyme, { shallow } from 'enzyme'
import Adapter from 'enzyme-adapter-react-16'
import CheckLogin from '../../../components/authentication/CheckLogin'

describe('<CheckLogin />', () => {
  Enzyme.configure({ adapter: new Adapter() })

  beforeEach(() => {
    jest.resetModules()
  })

  it('should render children elements if authentication is true', () => {
    const props = {
      children: <div className='auth'>Authentication successful</div>,
      context: { tmtAuth: {}, isAuthenticated: true },
    }

    const wrapper = shallow(<CheckLogin {...props} />)

    expect(wrapper.find('div.auth').length).toBe(1)
  })

  it('should not render children elements if authentication is false', () => {
    const props = {
      children: <div className='auth'>Authentication successful</div>,
      context: { tmtAuth: null, isAuthenticated: false },
    }

    const wrapper = shallow(<CheckLogin {...props} />)

    expect(wrapper.find('div.auth').length).toBe(0)
  })
})
