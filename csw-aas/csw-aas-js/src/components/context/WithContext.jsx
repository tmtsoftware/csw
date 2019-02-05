import React from 'react'
import { Consumer } from './AuthContext'

/**
 * Higher order function which returns Component with additional props e.g auth
 * login and logout used through Consumer of context
 * @param Component react component with some props
 */
const WithContext = Component => {
  return props => {
    return (
      <Consumer>
        {({ auth, login, logout }) => {
          return (
            <Component
              {...props}
              auth={auth}
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
