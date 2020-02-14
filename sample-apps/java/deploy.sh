#!/bin/bash 

APP_NAME=hello-world-${RANDOM}
JAR_NAME=app.jar
spring jar  ${JAR_NAME} hello.groovy 
cf push -p ${JAR_NAME} APP_NAME
echo "the application named ${APP_NAME} has been deployed."