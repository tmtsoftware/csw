This project was bootstrapped with [Create React App](https://github.com/facebook/create-react-app).

Please update react/app/public/keycloak.json file for following:
```json
"auth-server-url": "http://{ip}:8081/auth"
``` 
Please replace {ip} with your own ip.
e.g. -
```json
"auth-server-url": "http://192.168.1.6:8081/auth",
``` 

## Available Scripts

In the project directory, you can run:

### `npm install`
Installs all dependencies mentioned in package.json

### `npm start`

Runs the app in the development mode.<br>
Open [http://localhost:3000](http://localhost:3000) to view it in the browser.

The page will reload if you make edits.<br>
You will also see any lint errors in the console.

