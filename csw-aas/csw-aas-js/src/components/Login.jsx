import React from 'react'
import PropTypes from 'prop-types'
import WithContext from './context/WithContext'

export class Login extends React.Component {
  render() {
    return <button onClick={async () => await this.props.login()}>Login</button>
  }
}

Login.propTypes = {
  login: PropTypes.func,
}

export default WithContext(Login)
