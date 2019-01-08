import React from 'react'
import PropTypes from 'prop-types'
import { Consumer } from '../context/TMTAuthContext'

const CheckLogin = ({ children, error }) => (
  <Consumer>
    {({ isAuthenticated }) => (isAuthenticated ? children : error)}
  </Consumer>
)

CheckLogin.propTypes = {
  children: PropTypes.node,
  error: PropTypes.node,
}

export default CheckLogin
