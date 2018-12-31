import React from 'react'
import PropTypes from 'prop-types'

class ResourceRole extends React.Component {
  render() {
    const { resourceRole, resource, children, context } = this.props
    return (<div className='card-content'>
      {(context.isAuthenticated && context.tmtAuth.hasResourceRole(resourceRole, resource)) ? children : null}
    </div>)
  }
}

ResourceRole.propTypes = {
  resourceRole: PropTypes.string.isRequired,
  resource: PropTypes.string,
  children: PropTypes.node,
  context: PropTypes.object.isRequired
}

export default ResourceRole
