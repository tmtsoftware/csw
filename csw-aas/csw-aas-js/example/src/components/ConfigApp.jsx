import React from 'react'
import {BrowserRouter, Route} from 'react-router-dom'

import {TMTAuthContext, Login, Logout, CheckLogin, RealmRole, ResourceRole} from 'csw-aas-js'
import NavComponent from './NavComponent'
import {AppConfig} from '../config/AppConfig'
import WriteConfig from './WriteConfig'
import ReadConfig from './ReadConfig'

class ConfigApp extends React.Component {
  constructor(props) {
    super(props);
    this.state = {authContext: {tmtAuth: null, isAuthenticated: false}}
  }

  render() {
    const config = {...AppConfig};
    return <div>
      <TMTAuthContext.Provider value={this.state.authContext}>
        <BrowserRouter>
          <div style={{'textAlign': 'center'}} className=' row card blue-grey darken-1 col s12 m7'>
            <NavComponent />
            <Route path='/login' render={(_) => (<Login config={config} onAuthentication={this.setAuthContext} />)} />
            <Route path='/logout' render={(_) => (<Logout tmtAuth={this.state.authContext.tmtAuth} onLogout={this.resetAuthContext} />)} />

            <Route exact path='/write'render={(_) => (<CheckLogin>
              <WriteConfig />
            </CheckLogin>)} />

            <Route exact path='/public' component={ReadConfig} />

          </div>
        </BrowserRouter>

        <RealmRole role='example-admin-role'>
          <div>Hello you authenticated for example-admin-role</div>
          <WriteConfig />
        </RealmRole>

        <RealmRole role='invalid-role'>
          <div>Hello you authenticated for invalid-role</div>
          <WriteConfig />
        </RealmRole>

        <ResourceRole role='person-role' resource='example-server'>
          <div>Hello you authenticated for person-role of example-server</div>
          <WriteConfig />
        </ResourceRole>

        <ResourceRole role='invalid-role'>
          <div>Hello you authenticated for invalid-role</div>
          <WriteConfig />
        </ResourceRole>

      </TMTAuthContext.Provider>
    </div>
  }

  setAuthContext = ({tmtAuth, isAuthenticated}) => {
    this.setState({authContext: {tmtAuth, isAuthenticated: isAuthenticated}})
  };

  resetAuthContext = () => {
    this.setState({authContext: {tmtAuth: null, isAuthenticated: false}})
  }
}

export default ConfigApp
