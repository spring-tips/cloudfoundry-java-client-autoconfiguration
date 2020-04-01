#!/bin/bash

APP_NAME=sample-python-app-${RANDOM}
cf push -p . --random-route ${APP_NAME}