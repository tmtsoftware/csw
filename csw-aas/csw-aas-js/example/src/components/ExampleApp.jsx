import React from 'react'
import { BrowserRouter, Route } from 'react-router-dom'
// #import-components-example
import { CheckLogin, RealmRole, ClientRole, AuthContextProvider } from 'csw-aas-js'
// #import-components-example
import NavComponent from './NavComponent'
import { AppConfig } from '../config/AppConfig'
import Write from './Write'
import Read from './Read'
import ExampleError from './ExampleError'

class ExampleApp extends React.Component {
  state = {
    authContext: {
      auth: null,
      isAuthenticated: false,
    },
  }

  render() {
    const config = { ...AppConfig }
    return (
      <div className='row card col s12 m7'>
        {// #AuthContextProvider-component-usage
          <AuthContextProvider config={config}>
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
            // #clientRole-component-usage
              <ClientRole
                clientRole='person-role'
                client='example-server'
                error={<ExampleError />}>
                <div>Person role specific functionality</div>
              </ClientRole>
            // #clientRole-component-usage
            }

          </AuthContextProvider>
        // #AuthContextProvider-component-usage
        }
      </div>
    )
  }
}

export default ExampleApp
