import React from 'react'
import {BrowserRouter} from 'react-router-dom'
import {ResourceRole, TMTAuthContextProvider} from 'csw-aas-js'
import NavComponent from './NavComponent'
import {AppConfig} from '../config/AppConfig'
import WriteConfig from './CreateConfig'
import ConfigError from './ConfigError'
import ListConfig from './ListConfig'
import GetConfig from './GetConfig'

class ConfigApp extends React.Component {
  state = {
    authContext: {
      tmtAuth: null,
      isAuthenticated: false,
    },
  }

  render() {
    const config = { ...AppConfig }
    return (
      <div className='row card col s12 m7'>
        <TMTAuthContextProvider config={config}>
          <BrowserRouter>
            <div>
              <NavComponent />
            </div>
          </BrowserRouter>
          <ListConfig />
          <GetConfig />
          <ResourceRole
            resourceRole='admin'
            resource='csw-config-server'
            error={<ConfigError />}>
            <WriteConfig />
          </ResourceRole>
        </TMTAuthContextProvider>
      </div>
    )
  }
}

export default ConfigApp
