import React from 'react'
import PropTypes from 'prop-types'
import { Consumer } from '../context/TMTAuthContext'

const ResourceRole = ({ resourceRole, resource, children, error }) => (
  <Consumer>
    { ({tmtAuth, isAuthenticated}) => (
      isAuthenticated() && tmtAuth.hasResourceRole(resourceRole, resource) ? children : error)
    }
  </Consumer>
)

ResourceRole.propTypes = {
  resourceRole: PropTypes.string.isRequired,
  resource: PropTypes.string,
  children: PropTypes.node,
  error: PropTypes.node
}

export default ResourceRole
