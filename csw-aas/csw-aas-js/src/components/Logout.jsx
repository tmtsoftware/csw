import React from 'react'
import { Consumer } from './context/TMTAuthContextConsumer'

const Logout = () => (
  <Consumer>
    { ({ logout, isAuthenticated }) => (
      isAuthenticated ? <button onClick={logout}>Logout</button> : null
    )}
  </Consumer>
)

export default Logout
