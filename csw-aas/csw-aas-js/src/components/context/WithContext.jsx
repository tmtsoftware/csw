import React from 'react'
import { Consumer } from './TMTAuthContext'

/**
 * Higher order function which returns Component with additional props e.g tmtAuth
 * login and logout used through Consumer of context
 * @param Component react component with some props
 * @returns higher order component with existing props and some additional props like
 * tmtAuth, login, logout
 * @constructor
 */
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
