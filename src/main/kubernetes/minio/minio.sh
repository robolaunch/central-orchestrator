#!/bin/bash
helm repo add minio https://charts.min.io/
kubectl create ns minio
helm install --namespace minio --set rootUser=admin,rootPassword=admin,replicas=1 minio minio/minio 
