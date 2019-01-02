import fetch from 'isomorphic-fetch'
import { Config } from '../config/configs'

export const resolveAAS = async () => {
  const response = await fetch(
    `${Config['location-server-url']}/location/resolve/${
      Config['AAS-server-name']
    }?within=5seconds`,
  )
  if (response.status === 200) {
    const a = await response.json()
    return a.uri
  }
  return null
}
