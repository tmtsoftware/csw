# -*- mode: ruby -*-
# vi: set ft=ruby :
$install_a_jdk_and_sbt = <<SCRIPT
apt-get update
echo "Installing open jdk 11"
sudo add-apt-repository ppa:openjdk-r/ppa -y  # only Ubuntu 17.4 and earlier
sudo apt update -y
sudo apt install openjdk-11-jdk -y
# sudo bash
# /usr/bin/printf '\xfe\xed\xfe\xed\x00\x00\x00\x02\x00\x00\x00\x00\xe2\x68\x6e\x45\xfb\x43\xdf\xa4\xd9\x92\xdd\x41\xce\xb6\xb2\x1c\x63\x30\xd7\x92' > /etc/ssl/certs/java/cacerts
# /var/lib/dpkg/info/ca-certificates-java.postinst configure
# exit
echo "Installing sbt 1.2.8"
cd /opt/
curl -s -O https://sbt-downloads.cdnedge.bluemix.net/releases/v1.2.8/sbt-1.2.8.tgz
tar xvzf sbt-1.2.8.tgz
echo "export PATH=/opt/sbt/bin:\$PATH" > /etc/profile.d/sbt.sh

cd /vagrant
sbt
SCRIPT

# Vagrantfile API/syntax version. Don't touch unless you know what you're doing!
VAGRANTFILE_API_VERSION = "2"

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
        config.vm.box = "ubuntu/trusty64"
        config.vm.provider "virtualbox" do |v|
                v.memory = 8192
                v.name = "sbt-csw"
        end
        config.vm.network "private_network", ip: "192.168.50.4"
        config.vm.provision "shell", inline: $install_a_jdk_and_sbt
end