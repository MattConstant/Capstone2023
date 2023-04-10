# :penguin: Penguino :penguin:
This is the main repository for our capstone 2023 project. 
## :warning: Authors: Matthieu Constant, Gabriel Moracca, Seiji Akakabe, Jeciel Benerayan :warning:



## Pulling submodules
When pulling a submodule for the first time, add a `--init` option.
```bash
git pull --recurse-submodules origin main

# Pull all submodules
git submodule update --init --recursive

# Pull just one submodule
git submodule update --init penguino-front
```

## Pushing to your branch
```bash
git checkout [Your branch name]

git add .

git commit -m "message"

git push
```
