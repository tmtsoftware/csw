import React from 'react'
import { BrowserRouter, Route } from 'react-router-dom'
// #import-components-example
import { CheckLogin, RealmRole, ResourceRole, TMTAuthContextProvider } from 'csw-aas-js'
// #import-components-example
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
              <Route
                exact
                path='/secured'
                render={_ => (
                  // #checkLogin-component-usage
                  <CheckLogin error={<ExampleError />}>
                    <Write />
                  </CheckLogin>
                  // #checkLogin-component-usage
                )}
              />
              <Route exact path='/public' component={Read} />
            </div>
          </BrowserRouter>

          {
            // #realmRole-component-usage
            <RealmRole realmRole='example-admin-role' error={<ExampleError />}>
              <div>Example admin role specific functionality</div>
            </RealmRole>
            // #realmRole-component-usage
          }
          {
            // #resourceRole-component-usage
            <ResourceRole
              resourceRole='person-role'
              resource='example-server'
              error={<ExampleError />}>
              <div>Person role specific functionality</div>
            </ResourceRole>
            // #resourceRole-component-usage
          }

        </TMTAuthContextProvider>
      </div>
    )
  }
}

export default ExampleApp
