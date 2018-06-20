FROM ubuntu:14.04

USER root

WORKDIR /

COPY TOPOLOGY.py /

RUN apt-get update && \
apt-get install git curl vim nano python ython-software-properties software-properties-common build-essential python-dev -y && \
git clone https://github.com/mininet/mininet.git && \
cd mininet && git checkout -b 2.2.2 && cd .. && \
chmod u+x mininet/util/install.sh && \
./mininet/util/install.sh -a
