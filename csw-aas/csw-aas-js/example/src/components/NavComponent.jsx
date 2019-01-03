import { Link } from 'react-router-dom'
import React from 'react'

class NavComponent extends React.Component {
  render() {
    return <nav className='indigo'>
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

        </ul>
      </div>
    </nav>
  }
}

export default NavComponent
