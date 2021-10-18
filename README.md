# Commit Viewer

The current project aims to put hands on Github API (only related with commits URL). The project does NOT use Git cli,
by Git cli I meant the executable git from machine.

The project provides the following commands:
* git clone <URL> 
  (the url must be similar to real git clone, e.g, https://github.com/shenggwang/commit_viewer.git, and it does not work with SSH)
* git checkout <branch name>
* git branch
  (the current command only works to show local branches)
* git log
  (that shows logs, it only shows last 30 commits)

From REST API part, you can execute with pagination like the following:
```shell
curl "localhost:8080/commits?url=https://github.com/shenggwang/commit_viewer.git&page=2&size=5"
```
The current response will be hard to read, you can also use postman with the following get request and see the json response.

## How to start

For simplicity the current project can be run locally or in docker container.
* locally requires Java 11 and Maven, then procedures are normal as running a java project.
* docker container requires only docker daemon and execute the following:
    * To start, you should first build the docker image.
    ```shell
    docker build -t commit_viewer .
    ```
    * Then you can run with following command. (Note: after running the following command, you can iterate immediately with program)
    ```shell
    docker run -it -p 8080:8080 -t commit_viewer
    ```
    * In addition, you can also do a remote debug on port 5000 by executing the following command:
    ```shell
    docker run -it -p 8080:8080 -p 5000:5000 -t commit_viewer
    ```
    * Finally, you should be able to remove dangled images and the built image with following commands:
    ```shell
    docker ps -a --format '{{.Image}}: {{.Names}}'
    # get container names to remove
    docker stop <name>; docker rm <name>
    # remove all dangled images
    docker rmi $(docker images -q -f dangling=true)
    ```
