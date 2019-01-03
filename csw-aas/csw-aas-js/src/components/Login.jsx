import React from 'react'
import { Consumer } from './context/TMTAuthContextConsumer'

const Login = () => (
  <Consumer>
    { ({ login, isAuthenticated }) => (
      isAuthenticated ? null : <button onClick={login}>Login</button>
    )}
  </Consumer>
)

export default Login
