FROM twtmt/centos-tmt:latest
ENV	HOME /root
ENV	LANG en_US.UTF-8
ENV	LC_ALL en_US.UTF-8
ADD . /source/csw/
WORKDIR /source/csw