import React from 'react'
import { Consumer } from 'csw-aas-js'

const Read = () => (
  // #Consumer-component-usage
  <Consumer>
    { ({tmtAuth}) => {
      return <div className='nav-wrapper'>
        { (tmtAuth && tmtAuth.isAuthenticated()) ? <div>
            Hello, you are logged in
          <div>Open functionality</div>
        </div>
          : <div>
            Hello, you are not logged in
            <div>Open functionality</div>
          </div>
        }
      </div>
    }
    }
  </Consumer>
  // #Consumer-component-usage
)

export default Read
