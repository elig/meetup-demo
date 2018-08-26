#!groovy

def info(message) {
    echo "INFO: ${message}"
}

def warn(message) {
    echo "WARNING: ${message}"
}

def error(message) {
    echo "ERROR: ${message}"
}

return this