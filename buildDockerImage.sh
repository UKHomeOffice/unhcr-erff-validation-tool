#!/bin/sh

echo "Getting version..."
APP_VERSION=`mvn help:evaluate -Dexpression=project.version -q -DforceStdout`
echo "Building version ${APP_VERSION}"

DOCKER_IMAGE=docker.digital.homeoffice.gov.uk/scala/play-scala-shopping-cart:v${APP_VERSION}

echo "Building package..."
mvn clean package

echo "Building Docker image..."
docker build . -t ${DOCKER_IMAGE}

echo "To run:"
echo docker container run -dp 8080:8080 -t ${DOCKER_IMAGE}
