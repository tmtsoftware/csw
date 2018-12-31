import React from 'react'
import PropTypes from 'prop-types'

const RealmRole = props => {
  const { realmRole, children, context, error } = props
  return (
    <div className='card-content'>
      {context.isAuthenticated && context.tmtAuth.hasRealmRole(realmRole)
        ? children
        : error}
    </div>
  )
}

RealmRole.propTypes = {
  realmRole: PropTypes.string.isRequired,
  children: PropTypes.node,
  context: PropTypes.object.isRequired,
  error: PropTypes.node
}

export default RealmRole
