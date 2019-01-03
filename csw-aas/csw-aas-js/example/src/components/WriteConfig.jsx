import React from 'react'
import { Consumer } from 'csw-aas-js'

class WriteConfig extends React.Component {
  state = { user: null }

  componentWillMount = async () => {
    return <Consumer>
      { async ({ tmtAuth }) => {
        if (tmtAuth) {
          let loadUserInfo = await tmtAuth.loadUserInfo()
          loadUserInfo.success(user => {
            this.setState({ user })
          })
        }
      }
      }
    </Consumer>
  }

  render() {
    return (
      <div>
        {this.state.user && `Hello ${this.state.user.preferred_username}`}
        <br />
        Secured functionality - Writing Config
      </div>
    )
  }
}

export default WriteConfig
