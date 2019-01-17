import fetch from 'isomorphic-fetch'
import { Config } from '../config/configs'

/*
 * URL constant which contains location service end point for resolving AAS server
 */
const URL = `${Config['location-server-url']}/location/resolve/${
  Config['AAS-server-name']
}?within=5seconds`

/**
 * Utility method responsible for resolving AAS server using location service
 * @param url default param which contains location service end point for resolving
 * AAS server
 * @returns AAS url if resolved from location service else return null
 */
export const resolveAAS = async function(url = URL) {
  const response = await fetch(url)
  if (response.status === 200) {
    const a = await response.json()
    return a.uri
  }
  return null
}
