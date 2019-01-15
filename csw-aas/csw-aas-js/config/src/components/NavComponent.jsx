import React from 'react'
import {Consumer, Login, Logout} from 'csw-aas-js'

class NavComponent extends React.Component {
  render() {
    return (
      <Consumer>

        { ({login, logout, tmtAuth, isAuthenticated}) => {
          return <nav className='indigo'>
            <div className='nav-wrapper'>
              <a href='https://www.tmt.org/' className='brand-logo'>
              TMT
              </a>
              <ul className='hide-on-med-and-down right'>
                <li>
                  { isAuthenticated() ? (
                    <Logout />
                  ) : (
                    <Login />
                  )}
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
