#!/bin/bash

nohup mvn clean compile exec:java@acl-apd-server &
sleep 20
mvn clean test checkstyle:checkstyle pmd:pmd
cp -r target /tmp/test/
