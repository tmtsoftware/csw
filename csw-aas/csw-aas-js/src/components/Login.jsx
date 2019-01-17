import React from 'react'
import PropTypes from 'prop-types'
import WithContext from './context/WithContext'

/**
 * React component which renders Login button.WithContext utility provides login method as a prop.
 * Call to this.props.login method is responsible for resolving and instantiating AAS server.
 */
export class Login extends React.Component {
  render() {
    return (
      <button
        onClick={async () => {
          await this.props.login()
        }}>
        Login
      </button>
    )
  }
}

Login.propTypes = {
  login: PropTypes.func,
}

export default WithContext(Login)
