import {Link} from "react-router-dom";
import React from "react";

class NavComponent extends React.Component{
    render(){
        return <div className="white-text">
            <Link style={{"color":"white"}} to='/'>Open Functionality</Link> | <Link style={{"color":"white"}} to='/secured'>Secured Functionality</Link>
        </div>
    }
}

export default NavComponent


