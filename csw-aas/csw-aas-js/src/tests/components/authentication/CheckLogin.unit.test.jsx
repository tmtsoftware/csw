import React from 'react'
import Enzyme, { mount } from 'enzyme'
import Adapter from 'enzyme-adapter-react-16'

describe('<CheckLogin />', () => {
  Enzyme.configure({ adapter: new Adapter() })

  beforeEach(() => {
    jest.resetModules()
  })

  it('should render children elements if authentication is true', () => {
    const getCheckLoginWithMockContext = () => {
      const mockContext = {
        tmtAuth: null,
        isAuthenticated: true,
        login: () => true,
        logout: () => true
      }
      jest.mock('../../../components/context/TMTAuthContext', () => {
        return ({
          Consumer: jest.fn().mockImplementation((props) => {
            return (
              props.children(mockContext)
            )
          })
        })
      }
      )
      return require('../../../components/authentication/CheckLogin').default
    }

    const CheckLoginComponent = getCheckLoginWithMockContext()

    const props = {
      children: <div className='auth'>Authentication successful</div>,
      error: <div className='error'>Authentication unsuccessful</div>
    }

    const wrapper = mount(<CheckLoginComponent {...props} />)

    expect(wrapper.find('div.auth').length).toBe(1)
    expect(wrapper.find('div.error').length).toBe(0)
  })

  it('should not render children elements if authentication is true', () => {
    const getCheckLoginWithMockContext = () => {
      const mockContext = {
        tmtAuth: null,
        isAuthenticated: false,
        login: () => true,
        logout: () => true
      }
      jest.mock('../../../components/context/TMTAuthContext', () => {
        return ({
          Consumer: jest.fn().mockImplementation((props) => {
            return (
              props.children(mockContext)
            )
          })
        })
      }
      )
      return require('../../../components/authentication/CheckLogin').default
    }

    const CheckLoginComponent = getCheckLoginWithMockContext()

    const props = {
      children: <div className='auth'>Authentication successful</div>,
      error: <div className='error'>Authentication unsuccessful</div>
    }

    const wrapper = mount(<CheckLoginComponent {...props} />)

    expect(wrapper.find('div.auth').length).toBe(0)
    expect(wrapper.find('div.error').length).toBe(1)
  })
})
