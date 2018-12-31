import React from 'react'
import { Logout } from '../../components/Logout'
import Enzyme, { shallow } from 'enzyme'
import Adapter from 'enzyme-adapter-react-16'

describe('<Logout />', () => {
  Enzyme.configure({ adapter: new Adapter() })

  it('should call logout', () => {
    const props = {
      tmtAuth: {
        logout: jest.fn()
      },
      onLogout: jest.fn(),
      history: {
        push: jest.fn()
      }
    }
    shallow(<Logout {...props} />)

    expect(props.history.push).toHaveBeenCalledWith('/')
    expect(props.tmtAuth.logout).toHaveBeenCalled()
    expect(props.onLogout).toHaveBeenCalled()
  })
})
