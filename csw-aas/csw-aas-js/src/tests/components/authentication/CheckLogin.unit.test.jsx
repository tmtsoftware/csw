import React from 'react'
import Enzyme, { mount } from 'enzyme'
import Adapter from 'enzyme-adapter-react-16'
import renderer from 'react-test-renderer'

// DEOPSCSW-630 - Javascript adapter for AAS
// DEOPSCSW-631 - React layer for javascript adapter for AAS
describe('<CheckLogin />', () => {
  Enzyme.configure({ adapter: new Adapter() })

  beforeEach(() => {
    jest.resetModules()
  })

  it('should render children elements if authentication is true', () => {
    const getCheckLoginWithMockContext = () => {
      const mockContext = {
        auth: {
          isAuthenticated: jest.fn().mockImplementation(() => {
            return true
          }),
        },
        login: () => true,
        logout: () => true,
      }
      jest.mock('../../../components/context/AuthContext', () => {
        return {
          Consumer: jest.fn().mockImplementation(props => {
            return props.children(mockContext)
          }),
        }
      })
      return require('../../../components/authentication/CheckLogin').default
    }

    const CheckLoginComponent = getCheckLoginWithMockContext()

    const props = {
      children: <div className='auth'>Authentication successful</div>,
      error: <div className='error'>Authentication unsuccessful</div>,
    }

    const wrapper = mount(<CheckLoginComponent {...props} />)

    expect(wrapper.find('div.auth').length).toBe(1)
    expect(wrapper.find('div.error').length).toBe(0)
  })

  it('should not render children elements if authentication is true', () => {
    const getCheckLoginWithMockContext = () => {
      const mockContext = {
        auth: {
          isAuthenticated: jest.fn().mockImplementation(() => {
            return false
          }),
        },
        login: () => true,
        logout: () => true,
      }
      jest.mock('../../../components/context/AuthContext', () => {
        return {
          Consumer: jest.fn().mockImplementation(props => {
            return props.children(mockContext)
          }),
        }
      })
      return require('../../../components/authentication/CheckLogin').default
    }

    const CheckLoginComponent = getCheckLoginWithMockContext()

    const props = {
      children: <div className='auth'>Authentication successful</div>,
      error: <div className='error'>Authentication unsuccessful</div>,
    }

    const wrapper = mount(<CheckLoginComponent {...props} />)

    expect(wrapper.find('div.auth').length).toBe(0)
    expect(wrapper.find('div.error').length).toBe(1)
  })

  it('should render CheckLogin if authentication is true', () => {
    const getCheckLoginWithMockContext = () => {
      const mockContext = {
        auth: {
          isAuthenticated: jest.fn().mockImplementation(() => {
            return true
          }),
        },
        login: () => true,
        logout: () => true,
      }
      jest.mock('../../../components/context/AuthContext', () => {
        return {
          Consumer: jest.fn().mockImplementation(props => {
            return props.children(mockContext)
          }),
        }
      })
      return require('../../../components/authentication/CheckLogin').default
    }

    const CheckLoginComponent = getCheckLoginWithMockContext()

    const props = {
      children: <div className='auth'>Authentication successful</div>,
      error: <div className='error'>Authentication unsuccessful</div>,
    }

    const checkLoginComponent = renderer
      .create(<CheckLoginComponent {...props} />)
      .toJSON()
    expect(checkLoginComponent).toMatchSnapshot()
  })

  it('should not render CheckLogin if authentication is false', () => {
    const getCheckLoginWithMockContext = () => {
      const mockContext = {
        auth: {
          isAuthenticated: jest.fn().mockImplementation(() => {
            return false
          }),
        },
        login: () => true,
        logout: () => true,
      }
      jest.mock('../../../components/context/AuthContext', () => {
        return {
          Consumer: jest.fn().mockImplementation(props => {
            return props.children(mockContext)
          }),
        }
      })
      return require('../../../components/authentication/CheckLogin').default
    }

    const CheckLoginComponent = getCheckLoginWithMockContext()

    const props = {
      children: <div className='auth'>Authentication successful</div>,
      error: <div className='error'>Authentication unsuccessful</div>,
    }

    const checkLoginComponent = renderer
      .create(<CheckLoginComponent {...props} />)
      .toJSON()
    expect(checkLoginComponent).toMatchSnapshot()
  })

  it('should render nothing if CheckLogin if authentication is false and error component is not provided', () => {
    const getCheckLoginWithMockContext = () => {
      const mockContext = {
        auth: {
          isAuthenticated: jest.fn().mockImplementation(() => {
            return false
          }),
        },
        login: () => true,
        logout: () => true,
      }
      jest.mock('../../../components/context/AuthContext', () => {
        return {
          Consumer: jest.fn().mockImplementation(props => {
            return props.children(mockContext)
          }),
        }
      })
      return require('../../../components/authentication/CheckLogin').default
    }

    const CheckLoginComponent = getCheckLoginWithMockContext()

    const props = {
      children: <div className='auth'>Authentication successful</div>,
    }

    const checkLoginComponent = renderer
      .create(<CheckLoginComponent {...props} />)
      .toJSON()
    expect(checkLoginComponent).toMatchSnapshot()
  })
})
