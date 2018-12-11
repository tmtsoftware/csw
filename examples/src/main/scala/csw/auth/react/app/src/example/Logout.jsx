import React from "react";
import {withRouter} from "react-router-dom";

class Logout extends React.Component{
    render(){
        return <button onClick={() => this.logout()} >Logout</button>
    }

    logout(){
        this.props.history.push("/");
        this.props.keycloak.logout();
    }
}

export default withRouter(Logout);