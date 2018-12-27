import React from 'react'
import Enzyme, {mount} from 'enzyme'
import Adapter from 'enzyme-adapter-react-16'
import ResourceRole from '../../../components/authorization/ResourceRole'

describe('<RealmRole />', () => {
  Enzyme.configure({ adapter: new Adapter() })

  beforeEach(() => {
    jest.resetModules()
  })

  it('should render children elements if authenticated user has specified resource role', () => {
    const getResourceRoleWithMockContext = () => {
      jest.mock('../../../components/TMTAuthContext.jsx')
      return require('../../../components/authorization/ResourceRole.jsx').default
    }

    const props = {
      resourceRole: 'person-role',
      resource: 'example-server',
      children: <div className='resource-role'>Authorization successful</div>
    }

    const ResourceRoleComponent = getResourceRoleWithMockContext()
    const wrapper = mount(<ResourceRoleComponent {...props} />)

    expect(wrapper.find('div.resource-role').length).toBe(1)
  })

  it('should not render children elements if un-authenticated', () => {
    const props = {
      resourceRole: 'some-role',
      resource: 'some-server',
      children: <div className='resource-role'>Authorization successful</div>
    }

    const wrapper = mount(<ResourceRole {...props} />)

    expect(wrapper.find('div.resource-role').length).toBe(0)
  })
})
