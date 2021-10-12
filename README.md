# Commit Viewer

## How to start
```shell
docker build -t commit_viewer .
docker run -it -p 8080:8080 -t commit_viewer
```

## How to debug
```shell
docker run -it -p 8080:8080 -p 5000:5000 -t commit_viewer
```

## How to remove
```shell
docker rmi $(docker images -q -f dangling=true)
docker image rm commit_viewer
```