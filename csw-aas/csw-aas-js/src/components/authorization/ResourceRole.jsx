import React from 'react'
import PropTypes from 'prop-types'

const ResourceRole = props => {
  const { resourceRole, resource, children, context, error } = props
  return (
    <div className='card-content'>
      {context.isAuthenticated &&
      context.tmtAuth.hasResourceRole(resourceRole, resource)
        ? children
        : error}
    </div>
  )
}

ResourceRole.propTypes = {
  resourceRole: PropTypes.string.isRequired,
  resource: PropTypes.string,
  children: PropTypes.node,
  context: PropTypes.object.isRequired,
  error: PropTypes.node
}

export default ResourceRole
