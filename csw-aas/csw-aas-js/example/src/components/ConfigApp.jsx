import React from 'react'
import {BrowserRouter, Route} from 'react-router-dom'

import {TMTAuthContext, Login, Logout, CheckLogin, RealmRole, ResourceRole} from 'csw-aas-js'
import NavComponent from './NavComponent'
import {AppConfig} from '../config/AppConfig'
import WriteConfig from './WriteConfig'
import ReadConfig from './ReadConfig'

class ConfigApp extends React.Component {
  constructor(props) {
    super(props)
    this.state = {authContext: {tmtAuth: null, isAuthenticated: false}}
  }

  render() {
    const config = {...AppConfig}
    return <div className='row card col s12 m7'>
      <TMTAuthContext.Provider value={this.state.authContext}>
        <BrowserRouter>
          <div>
            <NavComponent />
            <Route path='/login' render={(_) => (<Login config={config} onAuthentication={this.setAuthContext} />)} />
            <Route path='/logout' render={(_) => (<Logout tmtAuth={this.state.authContext.tmtAuth} onLogout={this.resetAuthContext} />)} />

            <Route exact path='/secured'render={(_) => (<CheckLogin>
              <WriteConfig />
            </CheckLogin>)} />

            <Route exact path='/public' component={ReadConfig} />

          </div>
        </BrowserRouter>

        <RealmRole realmRole='example-admin-role'>
          <div>Example admin role specific functionality</div>
        </RealmRole>

        <RealmRole realmRole='invalid-role'>
          <div>Hello you authenticated for invalid-role</div>
        </RealmRole>

        <ResourceRole resourceRole='person-role' resource='example-server'>
          <div>Person role specific functionality</div>
        </ResourceRole>

        <ResourceRole resourceRole='invalid-role'>
          <div>Hello you authenticated for invalid-role</div>
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