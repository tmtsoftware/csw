import React from 'react'
import {Consumer, Login, Logout} from 'csw-aas-js'

class NavComponent extends React.Component {
  render() {
    return (
      <Consumer>
        { ({auth}) => {
          return <nav className='indigo'>
            <div className='nav-wrapper'>
              <a href='https://www.tmt.org/' className='brand-logo'>
              TMT
              </a>
              <ul className='hide-on-med-and-down right'>
                <li>
                  { (auth == null || auth === undefined) ? <span>Loading...</span>
                    : (auth.isAuthenticated() ? <Logout /> : <Login />)
                  }
                </li>
              </ul>
            </div>
          </nav>
        }
        }
      </Consumer>)
  }
}

export default NavComponent
