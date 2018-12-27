import React from 'react'
import {withRouter} from 'react-router-dom'
import PropTypes from 'prop-types'

export class Logout extends React.Component {
  render() {
    return null
  }

  componentWillMount = () => {
    this.props.history.push('/')
    this.props.tmtAuth.logout()
    this.props.onLogout()
  }
}

Logout.propTypes = {
  history: PropTypes.shape({
    push: PropTypes.func.isRequired
  }).isRequired,
  tmtAuth: PropTypes.object,
  onLogout: PropTypes.func
}

export default withRouter(Logout)
