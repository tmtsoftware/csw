import React from "react";
import KeyCloak from 'keycloak-js';
import Logout from "../example/Logout";
import CreatePerson from "../example/CreatePerson"

class Secured extends React.Component {

    constructor(props) {
        super(props);
        this.state = {keycloak: null, authenticated: false};
    }

    async componentWillMount(){
        const keycloak = KeyCloak('/keycloak.json');
        keycloak.onTokenExpired = () => {
            keycloak.updateToken(0)
                .success(function () {
                  console.log("token refreshed successfully")
                })
                .error(function() {
               alert('Failed to refresh the token, or the session has expired');
             });
        };
        const authenticated = await keycloak.init({onLoad: 'login-required', flow: 'hybrid'});
        this.setState({keycloak, authenticated})
    }

    render() {
        if(this.state.keycloak){
            if(this.state.authenticated){
            return <div className="card-content white-text">
                <h3>Secured Functionality</h3>
                <div>
                    This is a keycloak secured page
                </div>
                <div>
                    <CreatePerson keycloak={this.state.keycloak}/>
                    <Logout keycloak={this.state.keycloak} />
                </div>
            </div>;
            }
            else
            {
                return <div>
                    <h3>Unable to authenticate!</h3>
                </div>
            }
        }
        else{
            return <div className="card-content white-text">
                <h3>Initialising keycloak....</h3>
                <div>Logging in</div>
            </div>
        }
    }
}

export default Secured