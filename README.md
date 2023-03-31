# Penguino

## Pulling submodules
When pulling a submodule for the first time, add a `--init` option.
```bash
git pull --recurse-submodules origin main

# Pull all submodules
git submodule update --init --recursive

# Pull just one submodule
git submodule update --init penguino-front
```
