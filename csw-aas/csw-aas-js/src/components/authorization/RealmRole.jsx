import React from 'react'
import PropTypes from 'prop-types'
import { Consumer } from '../context/TMTAuthContext'

const RealmRole = ({ realmRole, children, error }) => (
  <Consumer>
    {({ tmtAuth, isAuthenticated }) =>
      isAuthenticated() && tmtAuth.hasRealmRole(realmRole) ? children : error
    }
  </Consumer>
)

RealmRole.propTypes = {
  realmRole: PropTypes.string.isRequired,
  children: PropTypes.node,
  error: PropTypes.node,
}

export default RealmRole
