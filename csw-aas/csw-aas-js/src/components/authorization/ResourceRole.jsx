import React from 'react'
import PropTypes from 'prop-types'

const ResourceRole = ({ resourceRole, resource, children, context, error }) => (
  <div>
    {context.isAuthenticated &&
    context.tmtAuth.hasResourceRole(resourceRole, resource)
      ? children
      : error}
  </div>
)

ResourceRole.propTypes = {
  resourceRole: PropTypes.string.isRequired,
  resource: PropTypes.string,
  children: PropTypes.node,
  context: PropTypes.object.isRequired,
  error: PropTypes.node,
}

export default ResourceRole
