import React from 'react'
import PropTypes from 'prop-types'
import { Consumer } from '../context/TMTAuthContext'

const ResourceRole = ({ resourceRole, resource, children, error }) => (
  <Consumer>
    {({ tmtAuth }) => {
      if (!tmtAuth) return error
      return tmtAuth.isAuthenticated() &&
        tmtAuth.hasResourceRole(resourceRole, resource)
        ? children
        : error || null
    }}
  </Consumer>
)

ResourceRole.propTypes = {
  resourceRole: PropTypes.string.isRequired,
  resource: PropTypes.string,
  children: PropTypes.node,
  error: PropTypes.node,
}

export default ResourceRole
