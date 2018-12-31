import React from 'react'
import PropTypes from 'prop-types'

const Error = props => {
  return <div>{props.children ? props.children : null}</div>
}

Error.propTypes = {
  children: PropTypes.node
}

export default Error
