import React from 'react'

/**
 * Default state for AuthContextProvider
 * @type {{auth: undefined, login: (function(): boolean), logout: (function(): boolean)}}
 */
const AuthContextDefaultState = {
  auth: undefined,
  login: () => true,
  logout: () => true,
}

const AuthContext = React.createContext(AuthContextDefaultState)
const { Provider, Consumer } = AuthContext

// todo: AuthContext is exported to support scala.js, see if only exporting Consumer works
export { AuthContext, Provider, Consumer, AuthContextDefaultState }
