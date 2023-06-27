#!/bin/bash

# To be executed from project root
docker build -t iudx/acl-apd-depl:latest -f docker/depl.dockerfile .
docker build -t iudx/acl-apd-dev:latest -f docker/dev.dockerfile .
