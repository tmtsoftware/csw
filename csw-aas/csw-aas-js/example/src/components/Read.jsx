import React from 'react'
import { Consumer } from 'csw-aas-js'

const Read = () => (
  // #Consumer-component-usage
  <Consumer>
    { ({auth}) => {
      return <div className='nav-wrapper'>
        { (auth && auth.isAuthenticated()) ? <div>
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
