# -*- mode: ruby -*-
# vi: set ft=ruby :
def create_synced_dir(config, host_dir, vm_dir, options = {})
  config.vm.synced_folder host_dir, vm_dir, options if File.directory?(host_dir)
end
# All Vagrant configuration is done below. The "2" in Vagrant.configure
# configures the configuration version (we support older styles for
# backwards compatibility). Please don't change it unless you know what
# you're doing.
Vagrant.configure(2) do |config|
  # The most common configuration options are documented and commented below.
  # For a complete reference, please see the online documentation at
  # https://docs.vagrantup.com.

  # Every Vagrant development environment requires a box. You can search for
  # boxes at https://atlas.hashicorp.com/search.
  config.vm.box = "ubuntu/trusty64"


   config.vm.network "forwarded_port", guest: 9092, host: 9092
   config.vm.network "forwarded_port", guest:9093, host:9093
   config.vm.network "forwarded_port", guest:9094, host:9094
   config.vm.network "forwarded_port", guest:7999, host:7999
   config.vm.network "forwarded_port", guest:8081, host:8081
   config.vm.network "forwarded_port", guest:4000, host:4000
   config.vm.network "forwarded_port", guest: 8500, host: 8500
   config.vm.network "forwarded_port", guest: 8400, host: 8400
   config.vm.network "forwarded_port", guest: 8600, host: 8600



  # Prometheus Server
  config.vm.network "forwarded_port", guest: 9090, host: 9090
  # Prometheus Alert Manager
  config.vm.network "forwarded_port", guest: 9093, host: 9093
  # Prometheus Dashboard
  config.vm.network "forwarded_port", guest: 3000, host: 3000

  # Disable automatic box update checking. If you disable this, then
  # boxes will only be checked for updates when the user runs
  # `vagrant box outdated`. This is not recommended.
  # config.vm.box_check_update = false

  # Create a forwarded port mapping which allows access to a specific port
  # within the machine from a port on the host machine. In the example below,
  # accessing "localhost:8080" will access port 80 on the guest machine.
  # config.vm.network "forwarded_port", guest: 80, host: 8080

  # Create a private network, which allows host-only access to the machine
  # using a specific IP.
  config.vm.network "private_network", ip: "192.168.33.10"
  user_home_directory = File.expand_path('~')
  create_synced_dir(config, "#{user_home_directory}/.ivy2", "/home/vagrant/.ivy2", { create: true })
  create_synced_dir(config, "./cache/images", "/var/docker-image-cache",{ create: true })



# Create a public network, which generally matched to bridged network.
  # Bridged networks make the machine appear as another physical device on
  # your network.
  # config.vm.network "public_network"

  # Share an additional folder to the guest VM. The first argument is
  # the path on the host to the actual folder. The second argument is
  # the path on the guest to mount the folder. And the optional third
  # argument is a set of non-required options.
  # config.vm.synced_folder "../data", "/vagrant_data"

  # Provider-specific configuration so you can fine-tune various
  # backing providers for Vagrant. These expose provider-specific options.
  # Example for VirtualBox:
  #
  config.vm.provider "virtualbox" do |vb|
  #   # Display the VirtualBox GUI when booting the machine
  #   vb.gui = true
  #
  #   # Customize the amount of memory on the VM:
      vb.memory = "4048"
      vb.cpus = 4
   end

  config.vm.provision "docker" do |docker|
  end
  config.vm.provision "get_dependencies", type: "shell",  path: "provisioning/get-dependencies.sh"
  config.vm.provision "docker_cache_images", type: "shell",  path: "provisioning/docker-cache-images.sh"
  
  #
  # View the documentation for the provider you are using for more
  # information on available options.

  # Define a Vagrant Push strategy for pushing to Atlas. Other push strategies
  # such as FTP and Heroku are also available. See the documentation at
  # https://docs.vagrantup.com/v2/push/atlas.html for more information.
  # config.push.define "atlas" do |push|
  #   push.app = "YOUR_ATLAS_USERNAME/YOUR_APPLICATION_NAME"
  # end

  # Enable provisioning with a shell script. Additional provisioners such as
  # Puppet, Chef, Ansible, Salt, and Docker are also available. Please see the
  # documentation for more information about their specific syntax and use.
  # config.vm.provision "shell", inline: <<-SHELL
  #   sudo apt-get update
  #   sudo apt-get install -y apache2
  # SHELL
end
