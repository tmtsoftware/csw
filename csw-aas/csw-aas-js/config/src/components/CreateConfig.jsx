import React from 'react'
import IOOperationComponent from './IOOperationComponent'
import { sPost } from './Client'
import { Consumer } from 'csw-aas-js'

class CreateConfig extends React.Component {
  state = { response: null, token: null, fileContent: '' }

  callBack = (res) => {
    this.setState({response: res})
  }

  createConfig = (input, token) => {
    sPost(
      `http://localhost:5000/config/${input}?comment="Sample commit message"`
      , this.callBack
      , token
      , this.state.fileContent)
  }

  updateFileContent = (event) => {
    this.setState({
      fileContent: event.target.value
    })
  }

  render() {
    return (
      <Consumer>
        {
          ({tmtAuth}) => {
            return (
              <div className='card-panel hoverable'>
                <IOOperationComponent token={tmtAuth.token} componentNameProp='Create Config' operation='Create Config' output={this.state.response} api={this.createConfig} />
                <div className='card-panel hoverable'>
                  File Content
                  <span>
                    <textarea value={this.state.fileContent} onChange={this.updateFileContent} />
                  </span>
                </div>
              </div>)
          }
        }
      </Consumer>
    )
  }
}

export default CreateConfig
