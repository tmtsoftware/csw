import React from 'react'
import { defaultState, Provider } from './TMTAuthContext'
import PropTypes from 'prop-types'
import { TMTAuth } from '../TMTAuth'

/**
 * React component which is wrapper over provider of react context api.
 * Responsible for instantiating keycloak and provide context value to consumers
 * props -
 * config json specific to UI application e.g. realm and clientId
 * children - react component or html element which can have consumer to access
 * context provided
 */
class TMTAuthContextProvider extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...defaultState, login: this.login, logout: this.logout }
  }

  async componentDidMount() {
    await this.loginWithoutRedirect()
  }

  render() {
    return <Provider value={this.state}>{this.props.children}</Provider>
  }

  /**
   * Instantiate keycloak and sets TMTAuthStore instance in state. This state can be provided
   * as a context
   */
  instantiateAAS = async (url, redirect) => {
    const { keycloak, authenticated } = await TMTAuth.authenticate(
      this.props.config,
      url,
      redirect,
    )
    authenticated
      .success(() => {
        const tmtAuth = TMTAuth.from(keycloak)
        this.setState({
          tmtAuth: tmtAuth,
        })
      })
      .error(() => {
        this.setState({ tmtAuth: null })
      })
  }

  /**
   * Resolves AAS server and instantiate keycloak in check-sso mode
   */
  loginWithoutRedirect = async () => {
    const url = await TMTAuth.getAASUrl()
    await this.instantiateAAS({ url: url }, false)
  }

  /**
   * Resolves AAS server and instantiate keycloak in login-required mode
   */
  login = async () => {
    const url = await TMTAuth.getAASUrl()
    await this.instantiateAAS({ url: url }, true)
  }

  logout = async () => {
    const logoutPromise = await this.state.tmtAuth.logout()
    logoutPromise.success(() => {
      this.setState({ tmtAuth: null })
    })
  }
}

TMTAuthContextProvider.propTypes = {
  config: PropTypes.object,
  children: PropTypes.node,
}

export default TMTAuthContextProvider