#!/bin/bash

APP_NAME=sample-python-app-${RANDOM}
cf push -p . ${APP_NAME}