import React from 'react'
import { withContext } from './context/TMTAuthContext'
import PropTypes from 'prop-types'

export class Login extends React.Component {
  componentDidMount = async () => {
    await this.props.login()
  }

  render() {
    return null
  }
}

Login.propTypes = {
  login: PropTypes.func,
}

export default withContext(Login)
