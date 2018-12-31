import React from 'react'
import PropTypes from 'prop-types'

const CheckLogin = props => {
  return (
    <div>{props.context.isAuthenticated ? props.children : props.error}</div>
  )
}

CheckLogin.propTypes = {
  children: PropTypes.node,
  context: PropTypes.object.isRequired,
  error: PropTypes.node
}

export default CheckLogin
