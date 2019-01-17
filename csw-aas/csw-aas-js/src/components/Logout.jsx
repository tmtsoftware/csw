import React from 'react'
import PropTypes from 'prop-types'
import WithContext from './context/WithContext'

/**
 * React component which renders Logout button. WithContext utility provides logout method as a prop.
 * Call to this.props.logout method is responsible for logging out.
 */
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
