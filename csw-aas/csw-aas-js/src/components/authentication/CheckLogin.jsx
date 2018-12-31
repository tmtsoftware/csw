import React from 'react'
import PropTypes from 'prop-types'

class CheckLogin extends React.Component {
  render() {
    return (
      <div>
        {this.props.context.isAuthenticated ? this.props.children : null}
      </div>
    )
  }
}

CheckLogin.propTypes = {
  children: PropTypes.node,
  context: PropTypes.object
}

export default CheckLogin
