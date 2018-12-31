import { Link } from 'react-router-dom'
import React from 'react'
import { TMTAuthContext } from './TMTAuthContext'

class NavComponent extends React.Component {
  // fixme: Is it required, if not then convert to functional stateless comp?
  static contextType = TMTAuthContext

  render() {
    return (
      <nav className='indigo'>
        <div className='nav-wrapper'>
          <a href='https://www.tmt.org/' className='brand-logo'>
            TMT
          </a>
          <ul className='hide-on-med-and-down right'>
            <li>
              <Link to='/public'> Public </Link>
            </li>
            <li>
              <Link to='/secured'> Secured </Link>
            </li>
            <li>
              {this.context.isAuthenticated ? (
                <Link to='/logout'> Logout </Link>
              ) : (
                <Link to='/login'> Login </Link>
              )}
            </li>
          </ul>
        </div>
      </nav>
    )
  }
}

export default NavComponent
