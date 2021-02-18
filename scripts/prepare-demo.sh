#!/bin/bash

set -euo pipefail

declare PROJECT="operator-test"

# stop any running operator
if [[ -z "$(kubectl get deploy/demo-operator 2>/dev/null)" && -n "$(kubectl get appops/my-bespoke-app 2>/dev/null)" ]]; then
    echo "Removing finalizers"
    # Need to manually remove the finalizer that the operator puts on the CR
    kubectl patch appops/my-bespoke-app -n operator-test --type='json' -p='[{"op": "replace", "path": "/metadata/finalizers", "value":[] }]'
fi 

# Delete namespace if it exists
kubectl get ns $PROJECT 2>/dev/null &&
    kubectl delete ns ${PROJECT}

# Delete the old CRD (we don't want to have versioning)
kubectl delete -f ${DEMO_HOME}/demo/kube/appops-crd.yaml

# above should not return until ns is actually deleted
kubectl create ns ${PROJECT}
kubectl config set-context --current --namespace ${PROJECT}

# Deploy the CRD
kubectl apply -f ${DEMO_HOME}/demo/kube/appops-crd.yaml

# Deploy the DemoApp
kubectl apply -f ${DEMO_HOME}/demo/kube/demo-app/demo-app-deploy.yaml
kubectl rollout status deploy/demo-app --timeout=6m
echo "demo-app successfully deployed"

# Deploy the Operator
kubectl apply -f ${DEMO_HOME}/demo/kube/operator-deploy.yaml
kubectl rollout status deploy/demo-operator --timeout=6m
echo "demo-operator successfully deployed"