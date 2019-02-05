import React from 'react'
import { AuthContextDefaultState, Provider } from './AuthContext'
import PropTypes from 'prop-types'
import { Auth } from '../Auth'

/**
 * React component which is wrapper over provider of react context api.
 * Responsible for instantiating keycloak and provide context value to consumers
 * props -
 * config json specific to UI application e.g. realm and clientId
 * children - react component or html element which can have consumer to access
 * context provided
 */
class AuthContextProvider extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...AuthContextDefaultState, login: this.login, logout: this.logout }
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
    const { keycloak, authenticated } = await Auth.authenticate(
      this.props.config,
      url,
      redirect,
    )
    authenticated
      .success(() => {
        const auth = Auth.from(keycloak)
        this.setState({
          auth: auth,
        })
      })
      .error(() => {
        this.setState({ auth: null })
      })
  }

  /**
   * Resolves AAS server and instantiate keycloak in check-sso mode
   */
  loginWithoutRedirect = async () => {
    const url = await Auth.getAASUrl()
    await this.instantiateAAS({ url: url }, false)
  }

  /**
   * Resolves AAS server and instantiate keycloak in login-required mode
   */
  login = async () => {
    const url = await Auth.getAASUrl()
    await this.instantiateAAS({ url: url }, true)
  }

  logout = async () => {
    const logoutPromise = await this.state.auth.logout()
    logoutPromise.success(() => {
      this.setState({ auth: null })
    })
  }
}

AuthContextProvider.propTypes = {
  config: PropTypes.object,
  children: PropTypes.node,
}

export default AuthContextProvider
