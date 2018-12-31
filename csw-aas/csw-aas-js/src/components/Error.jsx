import React from 'react'
import PropTypes from 'prop-types'

class Error extends React.Component {
  render() {
    return <div>{this.props.children ? this.props.children : null}</div>
  }
}

Error.propTypes = {
  children: PropTypes.node
}

export default Error
