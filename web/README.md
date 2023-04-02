# Web Deployment on an on-premise server
## Deploying web services from git pull
```bash
git pull origin main
cd web

# Pull services from the submodule.
git submodule update --init penguino-front
git submodule update --init PenguinoBackend

# deploy all services
docker compose up --build
```