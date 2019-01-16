import React from 'react'
import PropTypes from 'prop-types'
import WithContext from './context/WithContext'

export class Logout extends React.Component {
  render() {
    return (
      <button
        onClick={async () => {
          await this.props.logout()
        }}>
        Logout
      </button>
    )
  }
}

Logout.propTypes = {
  logout: PropTypes.func,
}

export default WithContext(Logout)
