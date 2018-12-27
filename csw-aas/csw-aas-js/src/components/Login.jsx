import React from 'react'
import PropTypes from 'prop-types'
import {Config} from '../config/configs.js'
import fetch from 'isomorphic-fetch'
import {TMTAuth} from './TMTAuth.jsx'

class Login extends React.Component {
  constructor(props) {
    super(props)
    this.state = {tmtAuth: null, isAuthenticated: false}
  }

  static async resolveAAS() {
    const response = await fetch(`${Config['location-server-url']}/location/resolve/${Config['AAS-server-name']}?within=5seconds`)
    let url = Config['AAS-server-url']
    if (response.status === 200) {
      const a = await response.json()
      url = a.uri
    }
    return url
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
     const url = await Login.resolveAAS()
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
