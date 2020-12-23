#!/bin/bash

declare MINIKUBE_HOST=192.168.86.87
# user on the minikube host
declare USER=mwh
# private key to be used to authenticate with MINIKUBE_HOST for USER
declare KEYFILE_PATH=${HOME}/.ssh/fedora

declare REMOTE_KUBECONFIG_PATH="/tmp/kubeconfig"
declare CERTS_DIR="/tmp/certs"
declare MINIKUBE_CTX_NAME="minikube"

if [[ -f $REMOTE_KUBECONFIG_PATH ]]; then
    echo "Removing old config file at ${REMOTE_KUBECONFIG_PATH}"
    rm ${REMOTE_KUBECONFIG_PATH}
fi

if [[ ! -d ${CERTS_DIR} ]]; then
    echo "Creating certs dir ${CERTS_DIR}"
    mkdir -p ${CERTS_DIR}
fi

# Assume this is run right after minikube is setup on host machine for user ${USER}
scp -i ${KEYFILE_PATH} ${USER}@${MINIKUBE_HOST}:~/.kube/config ${REMOTE_KUBECONFIG_PATH}

# find the host directory for certs.  For kubectl config documentation and examples, see
# this site: https://kubernetes.io/docs/reference/kubectl/cheatsheet/#kubectl-context-and-configuration
CA_CRT=$(kubectl config view -o jsonpath="{.clusters[?(@.name == \"${MINIKUBE_CTX_NAME}\")].cluster.certificate-authority}" --kubeconfig=${REMOTE_KUBECONFIG_PATH})
kubectl config set clusters.${MINIKUBE_CTX_NAME}.certificate-authority "${CERTS_DIR}/$(basename ${CA_CRT})" --kubeconfig=${REMOTE_KUBECONFIG_PATH}
CLIENT_CRT=$(kubectl config view -o jsonpath="{.users[?(@.name == \"${MINIKUBE_CTX_NAME}\")].user.client-certificate}" --kubeconfig=${REMOTE_KUBECONFIG_PATH})
kubectl config set users.${MINIKUBE_CTX_NAME}.client-certificate "${CERTS_DIR}/$(basename ${CLIENT_CRT})" --kubeconfig=${REMOTE_KUBECONFIG_PATH}
CLIENT_KEY=$(kubectl config view -o jsonpath="{.users[?(@.name == \"${MINIKUBE_CTX_NAME}\")].user.client-key}" --kubeconfig=${REMOTE_KUBECONFIG_PATH})
kubectl config set users.${MINIKUBE_CTX_NAME}.client-key "${CERTS_DIR}/$(basename ${CLIENT_KEY})" --kubeconfig=${REMOTE_KUBECONFIG_PATH}

declare FILES=( ${CA_CRT} ${CLIENT_CRT} ${CLIENT_KEY} )
for HOST_FILE_PATH in ${FILES[@]}; do
    FILE_NAME=$(basename ${HOST_FILE_PATH})
    REMOTE_CERT_FILE_PATH="${CERTS_DIR}/${FILE_NAME}"
    
    echo "Copying ${FILE} from ${REMOTE_KUBECONFIG_PATH} to ${REMOTE_CERT_FILE_PATH}."
    scp -i ${KEYFILE_PATH} ${USER}@${MINIKUBE_HOST}:${HOST_FILE_PATH} ${REMOTE_CERT_FILE_PATH}
done



# Reset the server on the config to the current host
kubectl config set clusters.${MINIKUBE_CTX_NAME}.server "https://${MINIKUBE_HOST}:8443" --kubeconfig=${REMOTE_KUBECONFIG_PATH}