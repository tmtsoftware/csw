import React from 'react'
import Enzyme, {shallow} from 'enzyme'
import Adapter from 'enzyme-adapter-react-16'
import RealmRole from '../../../components/authorization/RealmRole'

describe('<RealmRole />', () => {
  Enzyme.configure({ adapter: new Adapter() })

  beforeEach(() => {
    jest.resetModules()
  })

  it('should render children elements if authenticated user has specified realm role', () => {
    const props = {
      realmRole: 'example-admin-role',
      children: <div className='realm-role'>Authorization successful</div>,
      context: {tmtAuth: {hasRealmRole: jest.fn().mockImplementation(
        () => { return true })},
      isAuthenticated: true}
    }

    const wrapper = shallow(<RealmRole {...props} />)

    expect(wrapper.find('div.realm-role').length).toBe(1)
  })

  it('should not render children elements if un-authenticated', () => {
    const props = {
      realmRole: 'invalid-realm-role',
      children: <div className='realm-role'>Authorization successful</div>,
      context: {tmtAuth: null,
        isAuthenticated: false}
    }

    const wrapper = shallow(<RealmRole {...props} />)

    expect(wrapper.find('div.realm-role').length).toBe(0)
  })

  it('should not render children elements if authenticated user does not have specified realm role', () => {
    const props = {
      realmRole: 'invalid-realm-role',
      children: <div className='realm-role'>Authorization successful</div>,
      context: {tmtAuth: {hasRealmRole: jest.fn().mockImplementation(
        () => { return false })},
      isAuthenticated: true}
    }

    const wrapper = shallow(<RealmRole {...props} />)

    expect(wrapper.find('div.realm-role').length).toBe(0)
  })
})
