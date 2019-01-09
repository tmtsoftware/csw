import React from 'react'
import IOOperationComponent from './IOOperationComponent'
import fetch from 'isomorphic-fetch'

class ListConfig extends React.Component {
  state = { response: '' }

  callBack = (res) => {
    this.setState({response: res})
  }

  listConfig = async () => {
    const response = await fetch(`http://localhost:5000/list`)
    if (response.status === 200) {
      const a = await response.json()
      this.setState({response: JSON.stringify(a)})
    }
  }

  render() {
    return (
      <IOOperationComponent componentNameProp='List Config' operation='List' output={this.state.response} api={this.listConfig} />
    )
  }
}

export default ListConfig
