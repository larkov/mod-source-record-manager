FROM folioci/alpine-jre-openjdk11:latest

USER root

ENV GLIBC_REPO=https://github.com/sgerrand/alpine-pkg-glibc
ENV GLIBC_VERSION=2.30-r0

#RUN set -ex &&
#    apk --update add libstdc++ curl ca-certificates &&
#    for pkg in glibc-${GLIBC_VERSION} glibc-bin-${GLIBC_VERSION};
#        do curl -sSL ${GLIBC_REPO}/releases/download/${GLIBC_VERSION}/${pkg}.apk -o /tmp/${pkg}.apk; done &&
#    apk add --allow-untrusted /tmp/*.apk &&
#    rm -v /tmp/*.apk &&
#    /usr/glibc-compat/sbin/ldconfig /lib /usr/glibc-compat/lib

ENV VERTICLE_FILE mod-source-record-manager-server-fat.jar

# Set the location of the verticles
ENV VERTICLE_HOME /usr/verticles

# Copy your fat jar to the container
COPY mod-source-record-manager-server/target/${VERTICLE_FILE} ${VERTICLE_HOME}/${VERTICLE_FILE}

# Expose this port locally in the container.
EXPOSE 8081
