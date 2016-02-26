#!/bin/bash

curl -L https://github.com/akitchin/docker-handbrake/archive/master.zip > master.zip
unzip master.zip
cd /docker-handbrake-master/nas-runner && mvn install
cd /docker-handbrake-master/nas-runner && mvn exec:java -Dexec.mainClass=com.hazmit.nas_runner.App -Dexec.args="/usr/bin/HandBrakeCLI /data/INCOMING"
