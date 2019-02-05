import React from 'react'
import Enzyme, { mount } from 'enzyme'
import Adapter from 'enzyme-adapter-react-16'
import renderer from 'react-test-renderer'

// DEOPSCSW-630 - Javascript adapter for AAS
// DEOPSCSW-636 - JS adapter support  for Authorization
describe('<ClientRole />', () => {
  Enzyme.configure({ adapter: new Adapter() })

  beforeEach(() => {
    jest.resetModules()
  })

  it('should render children elements if authentication is true and with valid Client role', () => {
    const getClientRoleWithMockContext = () => {
      const mockContext = {
        auth: {
          hasResourceRole: jest.fn().mockImplementation(() => {
            return true
          }),
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
      return require('../../../components/authorization/ClientRole').default
    }

    const ClientRoleComponent = getClientRoleWithMockContext()

    const props = {
      children: <div className='client-role'>Authentication successful</div>,
      error: <div className='error'>Authentication unsuccessful</div>,
      clientRole: 'test-client-role',
      client: 'test-client',
    }

    const wrapper = mount(<ClientRoleComponent {...props} />)

    expect(wrapper.find('div.client-role').length).toBe(1)
    expect(wrapper.find('div.error').length).toBe(0)
  })

  it('should not render children elements if authentication is true but invalid Client role', () => {
    const getClientRoleWithMockContext = () => {
      const mockContext = {
        auth: {
          hasResourceRole: jest.fn().mockImplementation(() => {
            return false
          }),
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
      return require('../../../components/authorization/ClientRole').default
    }

    const ClientRoleComponent = getClientRoleWithMockContext()

    const props = {
      children: <div className='client-role'>Authentication successful</div>,
      error: <div className='error'>Authentication unsuccessful</div>,
      clientRole: 'invalid-client-role',
      client: 'invalid-client',
    }

    const wrapper = mount(<ClientRoleComponent {...props} />)

    expect(wrapper.find('div.client-role').length).toBe(0)
    expect(wrapper.find('div.error').length).toBe(1)
  })

  it('should not render children elements if authentication is false ', () => {
    const getClientRoleWithMockContext = () => {
      const mockContext = {
        auth: {
          hasResourceRole: jest.fn().mockImplementation(() => {
            return true
          }),
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
      return require('../../../components/authorization/ClientRole').default
    }

    const CLientRoleComponent = getClientRoleWithMockContext()

    const props = {
      children: <div className='client-role'>Authentication successful</div>,
      error: <div className='error'>Authentication unsuccessful</div>,
      clientRole: 'invalid-client-role',
      client: 'invalid-client',
    }

    const wrapper = mount(<CLientRoleComponent {...props} />)

    expect(wrapper.find('div.client-role').length).toBe(0)
    expect(wrapper.find('div.error').length).toBe(1)
  })

  it('should render ClientRole if authentication is true and with valid Client role', () => {
    const getClientRoleWithMockContext = () => {
      const mockContext = {
        auth: {
          hasResourceRole: jest.fn().mockImplementation(() => {
            return true
          }),
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
      return require('../../../components/authorization/ClientRole').default
    }

    const ClientRoleComponent = getClientRoleWithMockContext()

    const props = {
      children: <div className='client-role'>Authentication successful</div>,
      error: <div className='error'>Authentication unsuccessful</div>,
      clientRole: 'test-client-role',
      client: 'test-client',
    }

    const component = renderer
      .create(<ClientRoleComponent {...props} />)
      .toJSON()
    expect(component).toMatchSnapshot()
  })

  it('should not render ClientRole if authentication is true but invalid Client role', () => {
    const getClientRoleWithMockContext = () => {
      const mockContext = {
        auth: {
          hasResourceRole: jest.fn().mockImplementation(() => {
            return false
          }),
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
      return require('../../../components/authorization/ClientRole').default
    }

    const ClientRoleComponent = getClientRoleWithMockContext()

    const props = {
      children: <div className='client-role'>Authentication successful</div>,
      error: <div className='error'>Authentication unsuccessful</div>,
      clientRole: 'invalid-client-role',
      client: 'invalid-client',
    }

    const component = renderer
      .create(<ClientRoleComponent {...props} />)
      .toJSON()
    expect(component).toMatchSnapshot()
  })

  it('should not render ClientRole if authentication is false ', () => {
    const getCleintRoleWithMockContext = () => {
      const mockContext = {
        auth: {
          hasResourceRole: jest.fn().mockImplementation(() => {
            return true
          }),
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
      return require('../../../components/authorization/ClientRole').default
    }

    const ClientRoleComponent = getCleintRoleWithMockContext()

    const props = {
      children: <div className='client-role'>Authentication successful</div>,
      error: <div className='error'>Authentication unsuccessful</div>,
      clientRole: 'invalid-client-role',
      client: 'invalid-client',
    }

    const component = renderer
      .create(<ClientRoleComponent {...props} />)
      .toJSON()
    expect(component).toMatchSnapshot()
  })

  it('should render nothing if authentication is false and error component is not provided', () => {
    const getClientRoleWithMockContext = () => {
      const mockContext = {
        auth: {
          hasResourceRole: jest.fn().mockImplementation(() => {
            return true
          }),
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
      return require('../../../components/authorization/ClientRole').default
    }

    const ClientRoleComponent = getClientRoleWithMockContext()

    const props = {
      children: <div className='client-role'>Authentication successful</div>,
      clientRole: 'invalid-client-role',
      client: 'invalid-client',
    }

    const component = renderer
      .create(<ClientRoleComponent {...props} />)
      .toJSON()
    expect(component).toMatchSnapshot()
  })
})
