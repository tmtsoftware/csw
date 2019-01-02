import React from 'react'
import PropTypes from 'prop-types'

const CheckLogin = ({ context, children, error }) => (
  <div>{context.isAuthenticated ? children : error}</div>
)

CheckLogin.propTypes = {
  children: PropTypes.node,
  context: PropTypes.object.isRequired,
  error: PropTypes.node,
}

export default CheckLogin
