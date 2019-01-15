import React from 'react'
import { withContext } from './context/TMTAuthContext'
import PropTypes from 'prop-types'

export class Logout extends React.Component {
  render() {
    return <button onClick={async() => { await this.props.logout() }}>Logout</button>
  }
}

Logout.propTypes = {
  logout: PropTypes.func,
}

export default withContext(Logout)
