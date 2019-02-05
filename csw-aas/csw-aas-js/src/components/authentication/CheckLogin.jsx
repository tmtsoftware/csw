import React from 'react'
import PropTypes from 'prop-types'
import { Consumer } from '../context/AuthContext'

/**
 * React component which renders children if authenticated and error if unauthenticated
 * @param children (optional prop) can be react components or html element which will be rendered
 * if user is authenticated
 * @param error (optional prop) can be react components or html element which will be rendered
 * if user is not authenticated
 */
const CheckLogin = ({ children, error }) => (
  <Consumer>
    {({ auth }) => {
      if (!auth) return error
      return auth.isAuthenticated() ? children : error || null
    }}
  </Consumer>
)

CheckLogin.propTypes = {
  children: PropTypes.node,
  error: PropTypes.node,
}

export default CheckLogin
