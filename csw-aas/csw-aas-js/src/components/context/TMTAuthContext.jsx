import React from 'react'

/**
 * Default state for TMTAuthContextProvider
 * @type {{tmtAuth: null, login: (function(): boolean), logout: (function(): boolean)}}
 */
const defaultState = {
  tmtAuth: null,
  login: () => true,
  logout: () => true,
}

const { Provider, Consumer } = React.createContext(defaultState)

export { Provider, Consumer, defaultState }
