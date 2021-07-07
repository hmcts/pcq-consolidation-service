# To use ccd-docker elements

## Purpose
Guidance on how to set up pcqtest locally using the updated docker images.

##### 1) Install https://stedolan.github.io/jq/ 

```bash
  sudo apt-get install jq
```

##### 2) Login to azure

```bash
  az login
  az acr login --name hmctspublic --subscription DCD-CNP-Prod
  az acr login --name hmctsprivate --subscription DCD-CNP-Prod
```

##### 3) Reset your docker images, containers etc. 
Removing images will result in downloading all images again which you may want to skip.
```bash
   docker image rm $(docker image ls -a -q)
   docker container rm $(docker container ls -a -q)
   docker volume rm $(docker volume ls -q)
```

##### 4) Run environments scripts
First run the CCD login script
```bash
   ./ccd login
```
For mac:
```bash
   source ./bin/set-environment-variables.sh
```
For linux
```bash
   source ./bin/linux-set-environment-variables.sh
```
In addition all scripts require the following environment variables to be set with the corresponding values from the confluence page at https://tools.hmcts.net/confluence/x/eQP3P
```bash
export IDAM_ADMIN_USER=<value of username>
export IDAM_ADMIN_PASSWORD=<value of password>
```

##### 4.1) setup the logstash
In order to work locally on pcq-consolidation-service you will need build ccd-logstash locally
```
   git clone git@github.com:hmcts/ccd-logstash.git
   cd ccd-logstash
   cp hub.Dockerfile Dockerfile
   docker build . -t ccd-logstash:latest 
```   

##### 5) Start up docker 
```bash
   docker network create compose_default
   docker pull hmcts/ccd-logstash
   ./ccd compose pull
   ./ccd compose build
   ./ccd compose up
```

##### 6) Create blob store container
Once docker has started run
```bash
   ./bin/document-management-store-create-blob-store-container.sh
```

##### 7) Restart dm-store, sidam-api containers
Restart the dm-store container
```bash
   ./ccd compose restart dm-store
   ./ccd compose restart sidam-api
```

##### 8) Setup IDAM data.
```bash
   ./bin/idam-client-setup.sh
```

To check the IDAM data, you can log into IDAM-web `http://localhost:8082/login` with `idamOwner@hmcts.net` and the password used for `IDAM_ADMIN_PASSWORD`.

##### 9) Generate roles, import xls

###### Create roles and users
```bash
   ./bin/ccd-add-all-roles.sh
```
You can check the user and roles on the IDAM-web by searching for `test@gmail.com` on Manager Users page.

###### Import xls
```bash
   ./bin/ccd-import-definition.sh ./docker/ccd-spreadsheets/PCQ-Example-CaseDefinition.xlsx
```
##### 10) Start your local service 

###### Log into PCQ-Test CCD application
Login to ccd on `http://localhost:3451`. Caseworker: `test@gmail.com / Pa55word11`.
