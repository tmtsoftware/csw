import React from 'react'
import Enzyme, {mount} from 'enzyme'
import Adapter from 'enzyme-adapter-react-16'
import RealmRole from '../../../components/authorization/RealmRole'

describe('<RealmRole />', () => {
  Enzyme.configure({ adapter: new Adapter() });

  beforeEach(() => {
    jest.resetModules()
  });

  it('should render children elements if authenticated user has specified realm role', () => {
    const getRealmRoleWithMockContext = () => {
      jest.mock('../../../components/TMTAuthContext.jsx');
      return require('../../../components/authorization/RealmRole.jsx').default
    };

    const props = {
      role: 'example-admin-role',
      children: <div className='realm-role'>Authorization successful</div>
    };

    const RealmRoleComponent = getRealmRoleWithMockContext();
    const wrapper = mount(<RealmRoleComponent {...props} />);

    expect(wrapper.find('div.realm-role').length).toBe(1)
  });


  it('should not render children elements if un-authenticated', () => {
    const props = {
      role: 'invalid-realm-role',
      children: <div className='realm-role'>Authorization successful</div>
    };

    const wrapper = mount(<RealmRole {...props} />);

    expect(wrapper.find('div.realm-role').length).toBe(0)
  })
});
