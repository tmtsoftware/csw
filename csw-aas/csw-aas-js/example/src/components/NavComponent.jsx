import {Link} from 'react-router-dom'
import React from 'react'
import {TMTAuthContext} from 'csw-aas-js'

class NavComponent extends React.Component {
  static contextType = TMTAuthContext;

  render() {
    return <div className='white-text'>
      <Link style={{'color': 'white'}} to='/public'> Public - Read Config</Link>
      <br />
      <Link style={{'color': 'white'}} to='/write'> Secured - Write Config </Link>
      <br />

      {this.context.isAuthenticated
        ? <Link style={{'color': 'white'}} to='/logout'> Logout </Link>
        : <Link style={{'color': 'white'}} to='/login'> Login </Link>}

      <br />
    </div>
  }
}

export default NavComponent
