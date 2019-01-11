import fetch from 'isomorphic-fetch'
import { Config } from '../config/configs'

const URL = `${Config['location-server-url']}/location/resolve/${
  Config['AAS-server-name']
}?within=5seconds`

export const resolveAAS = async function(url = URL) {
  const response = await fetch(url)
  if (response.status === 200) {
    const a = await response.json()
    return a.uri
  }
  return null
}
