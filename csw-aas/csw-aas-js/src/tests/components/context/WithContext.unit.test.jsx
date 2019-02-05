import React from 'react'
import Enzyme, { shallow } from 'enzyme'
import Adapter from 'enzyme-adapter-react-16'

describe('<WithContext />', () => {
  Enzyme.configure({ adapter: new Adapter() })

  beforeEach(() => {
    jest.resetModules()
  })

  it('should render component with additional props', async () => {
    const getWithContextWithMockConsumer = () => {
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
      return require('../../../components/context/WithContext').default
    }

    const withContext = getWithContextWithMockConsumer()

    class SimpleComponent extends React.Component {
      render() {
        return <div>Some div </div>
      }
    }

    const ComponentWithContext = withContext(SimpleComponent)

    const wrapper = await shallow(<ComponentWithContext />)

    const props = wrapper.dive().props()

    expect(props['login']()).toBe(true)
    expect(props['logout']()).toBe(true)
    expect(props['auth'].isAuthenticated()).toBe(true)
  })
})
