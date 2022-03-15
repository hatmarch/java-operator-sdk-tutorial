# See here for image contents: https://github.com/microsoft/vscode-dev-containers/tree/v0.195.0/containers/go/.devcontainer/base.Dockerfile
# [Choice] Go version (use -bullseye variants on local arm64/Apple Silicon): 1, 1.16, 1.17, 1-bullseye, 1.16-bullseye, 1.17-bullseye, 1-buster, 1.16-buster, 1.17-buster
ARG VARIANT=11-bullseye
FROM mcr.microsoft.com/vscode/devcontainers/java:0-${VARIANT}

# [Choice] Node.js version: lts/*, 16, 14, 12, 10
ARG NODE_VERSION="lts/*"
RUN if [ "${NODE_VERSION}" != "none" ]; then su vscode -c ". /usr/local/share/nvm/nvm.sh && nvm install ${NODE_VERSION} 2>&1"; fi

# [Option] Install Maven
ARG INSTALL_MAVEN="true"
ARG MAVEN_VERSION="3.6.2"
RUN if [ "${INSTALL_MAVEN}" = "true" ]; then su vscode -c "umask 0002 && . /usr/local/sdkman/bin/sdkman-init.sh && sdk install maven \"${MAVEN_VERSION}\""; fi


# Or your actual UID, GID on Linux if not the default 1000
# NOTE: USERNAME is a reserved variable in zsh and cannot be overridden by ARG value
ARG USER_NAME="vscode"
ARG USER_UID=1000
ARG USER_GID="${USER_UID}"

# sudo installed for sake of docker (see below)
# Verify git, process tools, installed
RUN apt-get update && DEBIAN_FRONTEND=noninteractive \
    && apt-get -y install --no-install-recommends apt-utils software-properties-common lsb-release dialog \
        sudo git less jq procps skopeo wget rsync zsh zplug apt-transport-https \
        ca-certificates curl gnupg python3-pip groff man httpie \
        gcc build-essential \
#        gcc glibc-devel zlib-devel gcc-c++ \
    
    # cleanup
    && apt-get autoremove -y \
    && apt-get clean -y \
    && rm -rf /var/lib/apt/lists/*

# graalvm for native compilation
# RUN PLAT=$(dpkg --print-architecture); \
#     curl -L https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-21.1.0/graalvm-ce-java11-linux-aarch64-21.1.0.tar.gz | \
#     tar -zxvf - -C /usr/local/ && /usr/local/graalvm-ce-java11-21.0.0.2/bin/gu install native-image && \
#     export GRAAL_HOME=/usr/local/graalvm-ce-java11-21.0.0.2 && \
#     ln -s $GRAAL_HOME/bin/native-image /usr/local/bin

# once installed use zsh, particularly for additional string substitution functionality
SHELL ["/usr/bin/zsh", "-c"]

# Setup apt providers starting with Docker
RUN curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg \
    && echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/debian $(lsb_release -cs) stable" \
    | tee /etc/apt/sources.list.d/docker.list > /dev/null 

# setup google providers (for installing kubectl)
RUN curl -fsSLo /usr/share/keyrings/kubernetes-archive-keyring.gpg https://packages.cloud.google.com/apt/doc/apt-key.gpg \
    && echo "deb [signed-by=/usr/share/keyrings/kubernetes-archive-keyring.gpg] https://apt.kubernetes.io/ kubernetes-xenial main" \
    | tee /etc/apt/sources.list.d/kubernetes.list > /dev/null

# setup helm provider
RUN curl https://baltocdn.com/helm/signing.asc | apt-key add - \
    && echo "deb https://baltocdn.com/helm/stable/debian/ all main" | sudo tee /etc/apt/sources.list.d/helm-stable-debian.list

# Configure apt (again with new sources) and install packages from those sources
RUN apt-get update \
    && apt-get -y install docker-ce-cli kubectl helm \
    # cleanup (again)
    && apt-get autoremove -y \
    && apt-get clean -y \
    && rm -rf /var/lib/apt/lists/*

# Additional sudo and docker setup
#
# Create a non-root user to use if preferred - see https://aka.ms/vscode-remote/containers/non-root-user.
RUN echo ${USER_NAME} ALL=\(root\) NOPASSWD:ALL > /etc/sudoers.d/${USER_NAME} \
    && chmod 0440 /etc/sudoers.d/${USER_NAME} \
    && groupadd docker && usermod -aG docker ${USER_NAME} && usermod -aG root ${USER_NAME}

# install the AWS cli
ARG ZIP_OUT="/tmp/awscliv2.zip"
RUN PLAT=${$(uname -m)/arm64/aarch64}; \
    curl -L "https://awscli.amazonaws.com/awscli-exe-linux-${PLAT}.zip" -o "${ZIP_OUT}" \
    && unzip "${ZIP_OUT}" && ./aws/install && rm "${ZIP_OUT}" && rm -rf ./aws

# install stern
ARG STERN_VERSION=1.21.0
RUN PLAT=$(dpkg --print-architecture); \
    curl -L https://github.com/stern/stern/releases/download/v${STERN_VERSION}/stern_${STERN_VERSION}_linux_${PLAT}.tar.gz | \
    tar -C /usr/local/bin -xzvf - stern && chmod 755 /usr/local/bin/stern

# [Optional] Uncomment this line to install global node packages.
# RUN su vscode -c "source /usr/local/share/nvm/nvm.sh && npm install -g <your-package-here>" 2>&1
#RUN npm i -g @antora/cli@2.3 @antora/site-generator-default@2.3 && npm rm --global npx && npm install --global npx && npm install --global gulp
RUN npm i -g @antora/cli@2.3 @antora/site-generator-default@2.3 && npm install --global gulp

USER $USER_NAME

# Subdirectory where local-config files should reside (matched to gitignore to ensure no secrets are checked in)
ENV CONFIG_SUBDIR "local-config"
ENV DEMO_HOME "/workspaces/java-operator-sdk-tutorial/"


COPY --chown=vscode .zshrc.example $HOME/.zshrc