import React from 'react'
import PropTypes from 'prop-types'
import WithContext from './context/WithContext'

/**
 * React component which renders Login button.
 */
export class Login extends React.Component {
  render() {
    return (
      <button
        onClick={async () => {
          // Call to this.props.login method is responsible for resolving and instantiating AAS server
          await this.props.login()
        }}>
        Login
      </button>
    )
  }
}

Login.propTypes = {
  // WithContext utility provides login method as a prop.
  login: PropTypes.func,
}

export default WithContext(Login)
