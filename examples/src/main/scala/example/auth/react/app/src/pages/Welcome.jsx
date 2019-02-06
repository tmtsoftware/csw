import React from "react";

class Welcome extends React.Component {

    sendGetRequest = () => {this.sendRequest("Get")};

    sendRequest = (method) => {
        setTimeout(async () => {
            const response =  await fetch("http://localhost:9003/person", {
                method: method
            });

            if(response.ok) {
                alert("Action successful")
            } else {
                alert("Action failed")
            }

        }, 3000);
    };

    render() {
        return <div className="card-content white-text">
            <h3>This is a public page</h3>
            <button onClick={this.sendGetRequest}>Get Person</button>
        </div>
    }
}

export default Welcome