import React from 'react'
import { defaultState, Provider } from './TMTAuthContext'
import PropTypes from 'prop-types'
import { TMTAuth } from '../TMTAuth'

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

  loginWithoutRedirect = async () => {
    const url = await TMTAuth.getAASUrl()
    await this.instantiateAAS({ url: url }, false)
  }

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
