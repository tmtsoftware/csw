import React from 'react'

const defaultState = {
  tmtAuth: null,
  isAuthenticated: () =>  false,
  login: () => true,
  logout: () => true
}

const { Provider, Consumer } = React.createContext(defaultState)

export { Provider, Consumer, defaultState }

export const withContext = (Component) => {
  return (props) => {
    return <Consumer>
      {({isAuthenticated, login, logout}) => {
        return <Component {...props} isAuthenticated={isAuthenticated} login={login} logout={logout} />
      }}
    </Consumer>
  }
}
