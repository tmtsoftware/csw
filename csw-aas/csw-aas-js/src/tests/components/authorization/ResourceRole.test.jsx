import React from 'react'
import Enzyme, { shallow } from 'enzyme'
import Adapter from 'enzyme-adapter-react-16'
import ResourceRole from '../../../components/authorization/ResourceRole'

describe('<RealmRole />', () => {
  Enzyme.configure({ adapter: new Adapter() })

  beforeEach(() => {
    jest.resetModules()
  })

  it('should render children elements if authenticated user has specified resource role', () => {
    const props = {
      resourceRole: 'person-role',
      resource: 'example-server',
      children: <div className='resource-role'>Authorization successful</div>,
      context: {
        tmtAuth: {
          hasResourceRole: jest.fn().mockImplementation(() => {
            return true
          })
        },
        isAuthenticated: true
      }
    }

    const wrapper = shallow(<ResourceRole {...props} />)

    expect(wrapper.find('div.resource-role').length).toBe(1)
  })

  it('should not render children elements if un-authenticated', () => {
    const props = {
      resourceRole: 'some-role',
      resource: 'some-server',
      children: <div className='resource-role'>Authorization successful</div>,
      context: { tmtAuth: null, isAuthenticated: false }
    }

    const wrapper = shallow(<ResourceRole {...props} />)

    expect(wrapper.find('div.resource-role').length).toBe(0)
  })

  it('should not render children elements if authenticated user does not have specified resource role', () => {
    const props = {
      resourceRole: 'person-role',
      resource: 'example-server',
      children: <div className='resource-role'>Authorization successful</div>,
      context: {
        tmtAuth: {
          hasResourceRole: jest.fn().mockImplementation(() => {
            return false
          })
        },
        isAuthenticated: true
      }
    }

    const wrapper = shallow(<ResourceRole {...props} />)

    expect(wrapper.find('div.resource-role').length).toBe(0)
  })
})
