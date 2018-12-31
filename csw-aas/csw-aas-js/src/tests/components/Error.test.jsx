import React from 'react'
import Enzyme, { shallow } from 'enzyme'
import Adapter from 'enzyme-adapter-react-16'
import Error from '../../components/Error'

describe('<Error />', () => {
  Enzyme.configure({ adapter: new Adapter() })

  beforeEach(() => {
    jest.resetModules()
  })

  it('should render children elements is passed as prop', () => {
    const props = {
      children: <div className='error'>Please login</div>
    }

    const wrapper = shallow(<Error {...props} />)

    expect(wrapper.find('div.error').length).toBe(1)
  })

  it('should not render children elements if prop is not passed', () => {
    const wrapper = shallow(<Error />)

    expect(wrapper.find('div.realm-role').length).toBe(0)
  })

  it('should not render children elements if wrong prop is passed', () => {
    const props = {
      some: <div className='error'>Please login</div>
    }

    const wrapper = shallow(<Error {...props} />)

    expect(wrapper.find('div.realm-role').length).toBe(0)
  })
})
