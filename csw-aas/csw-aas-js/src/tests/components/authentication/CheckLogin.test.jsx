import React from 'react'
import Enzyme, {mount} from 'enzyme'
import Adapter from 'enzyme-adapter-react-16'
import CheckLogin from '../../../components/authentication/CheckLogin'

describe('<CheckLogin />', () => {
  Enzyme.configure({ adapter: new Adapter() });

  beforeEach(() => {
    jest.resetModules()
  });

  it('should render children elements if authentication is true', () => {
    const getCheckLoginWithMockContext = () => {
      jest.mock('../../../components/TMTAuthContext.jsx');
      return require('../../../components/authentication/CheckLogin.jsx').default
    };

    const props = {
      children: <div className='auth'>Authentication successful</div>
    };

    const CheckLoginComponent = getCheckLoginWithMockContext();
    const wrapper = mount(<CheckLoginComponent {...props} />);

    expect(wrapper.find('div.auth').length).toBe(1)
  });

  it('should not render children elements if authentication is false', () => {
    const props = {
      children: <div className='auth'>Authentication successful</div>
    };

    const wrapper = mount(<CheckLogin {...props} />);

    expect(wrapper.find('div.auth').length).toBe(0)
  })
});
