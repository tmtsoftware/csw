import React from 'react'
import PropTypes from 'prop-types'
import {TMTAuthContext} from '../TMTAuthContext.jsx'

class CheckLogin extends React.Component {
  render() {
    return (
      <TMTAuthContext.Consumer>
        {context => {
          return (
            <div>
              {context.isAuthenticated ? this.props.children : null}
            </div>
          )
        }}
      </TMTAuthContext.Consumer>
    )
  }
}

CheckLogin.propTypes = {
  children: PropTypes.node
};

CheckLogin.contextType = TMTAuthContext;

export default CheckLogin
