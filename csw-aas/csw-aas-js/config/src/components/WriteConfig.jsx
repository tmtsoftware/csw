import React from 'react'
import IOOperationComponent from './IOOperationComponent'
import { sPost } from './Client'
import { Consumer } from 'csw-aas-js'

class CreateConfig extends React.Component {
  state = { response: null, token: null }

  callBack = (res) => {
    this.setState({response: res})
  }

  createConfig = (input) => {
    sPost(`http://localhost:5000/config/${input}?comment="Sample commit message"`, this.callBack, this.state.token)
  }

  render() {
    return (
      <Consumer>
        {
          ({tmtAuth}) => {
            this.setState({token: tmtAuth.token})
            return <IOOperationComponent componentNameProp='Create Config' operation='Create Config' output={this.state.response} api={this.createConfig} />
          }
        }
      </Consumer>
    )
  }
}

export default CreateConfig
