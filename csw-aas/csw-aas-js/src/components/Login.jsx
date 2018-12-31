import React from 'react'
import PropTypes from 'prop-types'
import { TMTAuth } from './TMTAuth.jsx'

class Login extends React.Component {
  instantiateAAS = async url => {
    const { keycloak, authenticated } = await TMTAuth.authenticate(
      this.props.config,
      url
    )
    authenticated
      .success(() => {
        const tmtAuth = TMTAuth.from(keycloak)
        this.props.onAuthentication({
          tmtAuth: tmtAuth,
          isAuthenticated: tmtAuth.isAuthenticated
        })
      })
      .error(() => {
        this.props.onAuthentication({ tmtAuth: null, isAuthenticated: false })
      })
  }

  componentWillMount = async () => {
    const url = await TMTAuth.resolveAAS()
    await this.instantiateAAS({ url: url })
  }

  render() {
    return null
  }
}

Login.propTypes = {
  config: PropTypes.object,
  onAuthentication: PropTypes.func.isRequired
}

export default Login
