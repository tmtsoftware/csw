import React from 'react'
import PropTypes from 'prop-types'

class RealmRole extends React.Component {
  render() {
    const { realmRole, children, context } = this.props
    return (
      <div className='card-content'>
        {(context.isAuthenticated && context.tmtAuth.hasRealmRole(realmRole)) ? children : null}
      </div>
    )
  }
}

RealmRole.propTypes = {
  realmRole: PropTypes.string.isRequired,
  children: PropTypes.node,
  context: PropTypes.object
}

export default RealmRole
