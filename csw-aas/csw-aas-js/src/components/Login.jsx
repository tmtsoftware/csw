import React from 'react'
import PropTypes from 'prop-types'
import {TMTAuth} from './TMTAuth.jsx'

class Login extends React.Component {
  constructor(props) {
    super(props)
    this.state = {tmtAuth: null, isAuthenticated: false}
  }

  instantiateAAS = async (url) => {
    const {keycloak, authenticated} = await TMTAuth.authenticate(this.props.config, url)
    authenticated.success(() => {
      const tmtAuth = TMTAuth.from(keycloak)
      this.setState({tmtAuth, isAuthenticated: tmtAuth.isAuthenticated})
      this.props.onAuthentication({tmtAuth: this.state.tmtAuth, isAuthenticated: this.state.isAuthenticated})
    }).error(() => {
      this.props.onAuthentication({tmtAuth: this.state.tmtAuth, isAuthenticated: this.state.isAuthenticated})
    })
  };

   componentWillMount = async () => {
     const url = await TMTAuth.resolveAAS()
     await this.instantiateAAS({url: url})
   };

   render() {
     return null
   }
}

Login.propTypes = {
  config: PropTypes.object,
  onAuthentication: PropTypes.func
}

export default Login
