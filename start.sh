#!/bin/bash
export DB_URL=jdbc:postgresql://localhost:5432/vexhibition
export DB_USERNAME=root
export DB_PASSWORD=1234
export JWT_SECRET_KEY=RmFrZVNlY3JldEtleUZvckpXVEFwcGxpY2F0aW9uVGVzdGluZw==
export JWT_EXPIRATION_TIME=3600000
export PYTHON_API_URL=http://localhost:8000
JAR=/home/ubuntu/BACKEND/build/libs/CLIP-0.0.1-SNAPSHOT.jar
nohup java -Xmx350m -jar $JAR > /home/ubuntu/backend.log 2>&1 &
source /home/ubuntu/AI/venv/bin/activate
cd /home/ubuntu/AI/SearchEngine
nohup python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 > /home/ubuntu/ai.log 2>&1 &
echo done
