import React from "react";
import {BrowserRouter, Route} from "react-router-dom";
import Welcome from "./Welcome";
import Secured from "./Secured";
import NavComponent from "./../example/NavComponent"

import '../main.css'

class Master extends React.Component {
    render() {
        return <BrowserRouter>
            <div style={{"textAlign":"center"}} className=" row card blue-grey darken-1 col s12 m7">
                <NavComponent/>
                <Route exact path="/" component={Welcome}/>
                <Route path="/secured" component={Secured}/>
            </div>
        </BrowserRouter>
    }
}

export default Master