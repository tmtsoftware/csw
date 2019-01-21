import { Link } from 'react-router-dom'
import React from 'react'
import { Consumer, Logout, Login } from 'csw-aas-js'

class NavComponent extends React.Component {
  render() {
    return (
      <Consumer>
        { ({tmtAuth}) => {
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
                <li>
                  { (tmtAuth == null || tmtAuth === undefined) ? <span>Loading...</span>
                    : (tmtAuth.isAuthenticated() ? <Logout /> : <Login />)
                  }
                </li>
              </ul>
            </div>
          </nav>
        }
        }
      </Consumer>
    )
  }
}

export default NavComponent
