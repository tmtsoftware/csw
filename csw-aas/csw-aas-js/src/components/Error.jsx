import React from 'react'
import PropTypes from 'prop-types'

const Error = ({ children }) => <div>{children || null}</div>

Error.propTypes = {
  children: PropTypes.node
}

export default Error
