import React from 'react'
import { Consumer } from './TMTAuthContext'

const WithContext = Component => {
  return props => {
    return (
      <Consumer>
        {({ tmtAuth, login, logout }) => {
          return (
            <Component
              {...props}
              tmtAuth={tmtAuth}
              login={login}
              logout={logout}
            />
          )
        }}
      </Consumer>
    )
  }
}

export default WithContext
