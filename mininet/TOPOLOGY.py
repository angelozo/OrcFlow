#!/usr/bin/python

import re
import sys

from mininet.net import Mininet
from mininet.node import Controller, RemoteController, OVSKernelSwitch, UserSwitch
from mininet.cli import CLI
from mininet.log import setLogLevel, info, error
from mininet.link import Link, TCLink, Intf
from mininet.util import quietRun

def topology():
    "Create a network."
    net = Mininet( controller=RemoteController, link=TCLink, switch=OVSKernelSwitch )

    print "*** Creating nodes"
    h1 = net.addHost( 'h1', mac='0a:0a:0a:0a:0a:01', ip='192.168.3.1/24' )
    h2 = net.addHost( 'h2', mac='0a:0a:0a:0a:0a:02', ip='192.168.3.2/24' )
    h3 = net.addHost( 'h3', mac='0a:0a:0a:0a:0a:03', ip='192.168.3.3/24' )
    h4 = net.addHost( 'h4', mac='0a:0a:0a:0a:0a:04', ip='192.168.3.4/24' )

    s1 = net.addSwitch( 's1', protocols='OpenFlow13', listenPort=6671, mac='00:00:00:00:00:01' )
    s2 = net.addSwitch( 's2', protocols='OpenFlow13', listenPort=6672, mac='00:00:00:00:00:02' )
    s3 = net.addSwitch( 's3', protocols='OpenFlow13', listenPort=6673, mac='00:00:00:00:00:03' )
    s4 = net.addSwitch( 's4', protocols='OpenFlow13', listenPort=6674, mac='00:00:00:00:00:04' )
    s5 = net.addSwitch( 's5', protocols='OpenFlow13', listenPort=6675, mac='00:00:00:00:00:05' )
    s6 = net.addSwitch( 's6', protocols='OpenFlow13', listenPort=6676, mac='00:00:00:00:00:06' )
    s7 = net.addSwitch( 's7', protocols='OpenFlow13', listenPort=6677, mac='00:00:00:00:00:07' )

    # c1 = net.addController( 'c1', controller=RemoteController, ip='127.0.0.1', port=6653 )

    print "*** Creating links"

    net.addLink(s1, s2, 1, 1)
    net.addLink(s1, s3, 2, 1)
    net.addLink(s2, s4, 2, 1)
    net.addLink(s2, s5, 3, 1)
    net.addLink(s3, s6, 2, 1)
    net.addLink(s3, s7, 3, 1)


    net.addLink(h1, s4, 0, 2)
    net.addLink(h2, s5, 0, 2)
    net.addLink(h3, s6, 0, 2)
    net.addLink(h4, s7, 0, 2)


    print "*** Starting network"

    net.build()
    c1.start()

    s1.start( [c1] )
    s2.start( [c1] )
    s3.start( [c1] )
    s4.start( [c1] )
    s5.start( [c1] )
    s6.start( [c1] )
    s7.start( [c1] )


    h1.cmdPrint('ping 192.168.3.4 -c 1')
    h2.cmdPrint('ping 192.168.3.1 -c 1')
    h3.cmdPrint('ping 192.168.3.1 -c 1')
    h4.cmdPrint('ping 192.168.3.1 -c 1')


    print "*** Running CLI"
    CLI( net )

    print "*** Stopping network"
    net.stop()

if __name__ == '__main__':
    setLogLevel( 'info' )

    topology()
