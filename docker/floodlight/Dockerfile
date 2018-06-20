FROM ubuntu:14.04

USER root

WORKDIR /

RUN apt-get update && \
apt-get install python python-software-properties software-properties-common -y && \
add-apt-repository ppa:webupd8team/java && \
apt-get update && \
echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | sudo /usr/bin/debconf-set-selections && \
apt-get install oracle-java8-installer build-essential ant maven python-dev git -y && \
git clone https://github.com/floodlight/floodlight.git && \
cd floodlight && \
git submodule init && \
git submodule update && \
ant
