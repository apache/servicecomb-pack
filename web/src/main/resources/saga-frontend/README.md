# Saga Web UI -  A Transaction viewer dashboard
This project is using [ngx-admin](https://github.com/akveo/ngx-admin), [here you can find documentation and other useful articles](https://akveo.github.io/nebular/docs/getting-started/what-is-nebular).

### How to run the UI
To install saga-frontend on your machine you need to have the following tools installed:

- Git - https://git-scm.com
- Node.js - https://nodejs.org. Please note the version should be >=8.9.3
- Npm - Node.js package manager, comes with Node.js. Please make sure npm version is >=5
- You might also need some specific native packages depending on your operating system like build-essential on Ubuntu
- Clone the repository

```
git clone https://github.com/apache/servicecomb-pack
```  

- Navigate to the frontend folder  

```
cd servicecomb-pack/saga-web/src/main/resources/saga-frontend && npm i
```  
- To run a local copy
```
npm start  
```  
Go to http://0.0.0.0:4200 or http://localhost:4200 in your browser.  
- To create a bundle in production mode
```
npm run build:prod
```  
This will clear up your **dist** folder (where release files are located) and generate a release build. Now you can copy the sources from the **dist** folder and simply put it under the **static** folder or under a web server.

