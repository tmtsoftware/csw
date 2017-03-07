FROM twtmt/centos-tmt:latest
ENV	HOME /root
ENV	LANG en_US.UTF-8
ENV	LC_ALL en_US.UTF-8
ENV NO_PROXY="*.local, 169.254/16"
ENV no_proxy="*.local, 169.254/16"
ADD . /source/csw/
WORKDIR /source/csw