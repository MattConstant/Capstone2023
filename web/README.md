# Web Deployment on an on-premise server
## Deploying web services from git pull
```bash
git pull origin main
cd web

# Checkout to the latest main branch of the sumbodule before updating
cd PenguinoBackend
git pull origin main
git checkout main

# This only updates the submodule in the archived branch
git submodule update --init PenguinoBackend

# deploy all services
docker compose up --build
```
