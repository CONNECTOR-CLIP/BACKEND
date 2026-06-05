#!/bin/bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH=$JAVA_HOME/bin:$PATH

export DB_URL=jdbc:postgresql://localhost:5432/vexhibition
export DB_USERNAME=root
export DB_PASSWORD=1234
export JWT_SECRET_KEY=RmFrZVNlY3JldEtleUZvckpXVEFwcGxpY2F0aW9uVGVzdGluZw==
export JWT_EXPIRATION_TIME=3600000
export PYTHON_API_URL=http://localhost:8000
./gradlew bootRun
