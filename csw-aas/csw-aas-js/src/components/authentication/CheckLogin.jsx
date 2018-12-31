import React from 'react'
import PropTypes from 'prop-types'

class CheckLogin extends React.Component {
  render() {
    return (
      <div>
        {this.props.context.isAuthenticated ? this.props.children : this.props.error}
      </div>
    )
  }
}

CheckLogin.propTypes = {
  children: PropTypes.node,
  context: PropTypes.object.isRequired,
  error: PropTypes.node
}

export default CheckLogin
