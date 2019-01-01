import React from 'react'
import { withRouter } from 'react-router-dom'
import PropTypes from 'prop-types'

export class Logout extends React.Component {
  componentWillMount = () => {
    this.props.history.push('/')
    this.props.tmtAuth.logout()
    this.props.onLogout()
  }

  render() {
    return null
  }
}

Logout.propTypes = {
  history: PropTypes.shape({
    push: PropTypes.func.isRequired,
  }).isRequired,
  tmtAuth: PropTypes.object,
  onLogout: PropTypes.func.isRequired,
}

export default withRouter(Logout)
