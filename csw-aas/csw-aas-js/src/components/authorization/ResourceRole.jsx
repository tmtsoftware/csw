import React from 'react'
import PropTypes from 'prop-types'
import { Consumer } from '../context/TMTAuthContext'

/**
 * React component which renders if user is authenticated and has specified resource role
 * @param resourceRole (required prop) string which specifies expected resource role
 * @param resource (optional prop) string which specifies expected resource.If not specified, `clientId` is used.
 * @param children (optional prop) can be react components or html element which will be rendered
 * if user is authenticated and has specified resource role
 * @param error (optional prop) can be react components or html element which will be rendered
 * if user is not authenticated or does not have specified resource role
 * @returns React component
 */
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
