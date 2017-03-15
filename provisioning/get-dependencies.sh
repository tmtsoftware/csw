#!/bin/bash

echo "Getting dependencies..."

sudo apt-get update
sudo apt-get install -y git software-properties-common python-software-properties

# install JDK 8
sudo bash -c "echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && \
add-apt-repository -y ppa:webupd8team/java && \
apt-get update && \
apt-get install -y oracle-java8-installer"

#install sbt-0.13.13
sudo curl -LO "https://dl.bintray.com/sbt/native-packages/sbt/0.13.13/sbt-0.13.13.tgz" && sudo tar -xzv  -f sbt-0.13.13.tgz -C /usr/local

sudo apt-get install iproute2
sudo apt-get install iperf

echo 'export PATH="/usr/local/sbt-launcher-packaging-0.13.13/bin/:$PATH"' >> /home/vagrant/.bashrc
