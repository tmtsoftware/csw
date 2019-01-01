import React from 'react'
import Enzyme, { mount } from 'enzyme'
import Adapter from 'enzyme-adapter-react-16'
import Login from '../../components/Login'
import { TMTAuth } from '../../components/TMTAuth.jsx'

jest.mock('../../components/TMTAuth.jsx')

describe('<Login />', () => {
  Enzyme.configure({ adapter: new Adapter() })

  it('should call logout', async () => {
    const props = {
      config: {
        realm: 'example',
        clientId: 'example-app',
      },
      onAuthentication: jest.fn(),
    }

    const resolveAASMock = jest.spyOn(TMTAuth, 'resolveAAS')
    const authenticateMock = jest.spyOn(TMTAuth, 'authenticate')

    await mount(<Login {...props} />)

    expect(resolveAASMock).toHaveBeenCalled()
    expect(authenticateMock).toHaveBeenCalledWith(props.config, {
      url: 'http://mockhost:mockport',
    })
  })
})
