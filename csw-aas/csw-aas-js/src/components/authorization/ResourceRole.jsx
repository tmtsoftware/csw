import React from 'react'
import PropTypes from 'prop-types'
import {TMTAuthContext} from '../TMTAuthContext.jsx'

class ResourceRole extends React.Component {
  render() {
    return (
      <TMTAuthContext.Consumer>
        {context => {
          return (
            <div className='card-content'>
              {(context.isAuthenticated && context.tmtAuth.hasResourceRole(this.props.role, this.props.resource)) ? this.props.children : null}
            </div>
          )
        }}
      </TMTAuthContext.Consumer>
    )
  }
}

ResourceRole.propTypes = {
  role: PropTypes.string.isRequired,
  resource: PropTypes.string,
  children: PropTypes.node
};

ResourceRole.contextType = TMTAuthContext;

export default ResourceRole
