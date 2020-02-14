#!/usr/bin/env python
from flask import Flask
import os

app = Flask(__name__)
cf_port = os.getenv("PORT")
if cf_port is None:
    cf_port = '5000'

@app.route('/')
def hello():
    return {'message': 'Hello, world'}

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=int(cf_port), debug=True)
