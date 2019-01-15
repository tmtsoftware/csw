import React from 'react'
import { withContext } from './context/TMTAuthContext'
import PropTypes from 'prop-types'

export class Login extends React.Component {

  render() {
    return <button onClick={async () => await this.props.login()}>Login</button>
  }
}

Login.propTypes = {
  login: PropTypes.func,
}

export default withContext(Login)
