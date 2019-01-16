import React from 'react'

const defaultState = {
  tmtAuth: null,
  login: () => true,
  logout: () => true,
}

const { Provider, Consumer } = React.createContext(defaultState)

export { Provider, Consumer, defaultState }
