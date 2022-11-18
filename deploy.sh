#!/bin/sh

cd infrastructure/cdk
cdk deploy --all --outputs-file target/output.json