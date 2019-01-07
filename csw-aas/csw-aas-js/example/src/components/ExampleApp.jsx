import React from 'react'
import { BrowserRouter, Route } from 'react-router-dom'
import { CheckLogin, TMTAuthContextProvider, Login, Logout, RealmRole, ResourceRole } from 'csw-aas-js'
import NavComponent from './NavComponent'
import { AppConfig } from '../config/AppConfig'
import Write from './Write'
import Read from './Read'
import ExampleError from './ExampleError'

class ExampleApp extends React.Component {
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
              <Route path='/login' component={Login} />
              <Route path='/logout' component={Logout} />
              <Route
                exact
                path='/secured'
                render={_ => (
                  <CheckLogin>
                    <Write />
                  </CheckLogin>
                )}
              />

              <Route exact path='/public' component={Read} />
            </div>
          </BrowserRouter>
          <RealmRole
            realmRole='example-admin-role'
            error={<ExampleError />}>
            <div>Example admin role specific functionality</div>
          </RealmRole>

          <RealmRole realmRole='invalid-role'>
            <div>Hello you authenticated for invalid-role</div>
          </RealmRole>

          <ResourceRole
            resourceRole='person-role'
            resource='example-server'
            error={<ExampleError />}>
            <div>Person role specific functionality</div>
          </ResourceRole>

          <ResourceRole
            resourceRole='invalid-role'>
            <div>Hello you authenticated for invalid-role</div>
          </ResourceRole>
        </TMTAuthContextProvider>
      </div>
    )
  }
}

export default ExampleApp
