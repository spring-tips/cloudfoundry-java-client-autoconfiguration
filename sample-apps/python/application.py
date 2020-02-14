#!/usr/bin/env python
from flask import Flask

app = Flask(__name__)

@app.route('/')
def hello_world():
    return 'Hello, World!'

if __name__ == "__main__":
    import os, sys, time, re
    port = int(os.getenv("PORT", 9090))
    app.run(host='0.0.0.0', port=port)
