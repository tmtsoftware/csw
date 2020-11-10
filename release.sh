#!/usr/bin/env bash

if [[ $# -ne 1 || $1 != v*  ]]; then
    echo "Single VERSION number argument prefixed with 'v' expected ..."
    exit 2
fi

VERSION=$1

if [[ $PROD == true ]]; then
    git tag "$VERSION"
    git push origin "$VERSION"
    echo "Successfully tagged with $VERSION"
else
    echo "Set PROD=true environment variable to release ..."
fi
