import React from 'react'
import { withContext } from './context/TMTAuthContext'
import PropTypes from 'prop-types'

export class Logout extends React.Component {
  componentWillMount = async () => {
    if (this.props.isAuthenticated) {
      await this.props.logout()
    }
  }

  render() {
    return null
  }
}

Logout.propTypes = {
  isAuthenticated: PropTypes.bool,
  logout: PropTypes.func,
}

export default withContext(Logout)
