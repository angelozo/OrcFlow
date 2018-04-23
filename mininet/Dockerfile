FROM ubuntu:14.04

USER root
WORKDIR /root

COPY ENTRYPOINT.sh /
COPY TOPOLOGY.py /root
COPY TOPOLOGY.py /

RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    iproute2 \
    iputils-ping \
    mininet \
    net-tools \
    tcpdump \
    vim \
    x11-xserver-utils \
    xterm \
 && rm -rf /var/lib/apt/lists/* \
 && chmod +x /ENTRYPOINT.sh \
 && service openvswitch-switch start \
 && ovs-vsctl set-manager ptcp:6640 \
 && service openvswitch-switch stop

EXPOSE 6633 6653 6640

ENTRYPOINT [ "/ENTRYPOINT.sh" ]
