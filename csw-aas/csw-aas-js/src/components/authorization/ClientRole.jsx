import React from 'react'
import PropTypes from 'prop-types'
import { Consumer } from '../context/AuthContext'

/**
 * React component which renders if user is authenticated and has specified client role
 * @param clientRole (required prop) string which specifies expected client role
 * @param client (optional prop) string which specifies expected client.If not specified, `clientId` is used.
 * @param children (optional prop) can be react components or html element which will be rendered
 * if user is authenticated and has specified client role
 * @param error (optional prop) can be react components or html element which will be rendered
 * if user is not authenticated or does not have specified client role
 * @returns React component
 */
const ClientRole = ({ clientRole, client, children, error }) => (
  <Consumer>
    {({ auth }) => {
      if (!auth) return error
      return auth.isAuthenticated() &&
        auth.hasResourceRole(clientRole, client)
        ? children
        : error || null
    }}
  </Consumer>
)

ClientRole.propTypes = {
  clientRole: PropTypes.string.isRequired,
  client: PropTypes.string,
  children: PropTypes.node,
  error: PropTypes.node,
}

export default ClientRole
