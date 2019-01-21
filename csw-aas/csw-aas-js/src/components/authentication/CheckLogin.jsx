import React from 'react'
import PropTypes from 'prop-types'
import { Consumer } from '../context/TMTAuthContext'

/**
 * React component which renders if user is authenticated
 * @param children (optional prop) can be react components or html element which will be rendered
 * if user is authenticated
 * @param error (optional prop) can be react components or html element which will be rendered
 * if user is not authenticated
 * @returns React component which renders children if authenticated and error if unauthenticated
 */
const CheckLogin = ({ children, error }) => (
  <Consumer>
    {({ tmtAuth }) => {
      if (!tmtAuth) return error
      return tmtAuth.isAuthenticated() ? children : error || null
    }}
  </Consumer>
)

CheckLogin.propTypes = {
  children: PropTypes.node,
  error: PropTypes.node,
}

export default CheckLogin
