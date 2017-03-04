FROM centos:7
ENV	HOME /root
ENV	LANG en_US.UTF-8
ENV	LC_ALL en_US.UTF-8

RUN yum install -y curl; yum upgrade -y; yum update -y;  yum clean all

ENV JDK_VERSION 8u11
ENV JDK_BUILD_VERSION b13
RUN mkdir -p /usr/local
RUN curl -LO "http://download.oracle.com/otn-pub/java/jdk/8u121-b13/e9e7ea248e2c4826b92b3f075a80e441/jdk-8u121-linux-x64.tar.gz" -H 'Cookie: oraclelicense=accept-securebackup-cookie' && tar -xvz -f jdk-8u121-linux-x64.tar.gz -C /usr/local
ENV JAVA_HOME /jdk1.8.0_121/bin/
RUN ls /usr/local/

RUN curl -LO "https://dl.bintray.com/sbt/native-packages/sbt/0.13.13/sbt-0.13.13.tgz" && tar -xzv  -f sbt-0.13.13.tgz -C /usr/local
ENV PATH="/usr/local/jdk1.8.0_121/bin/:/usr/local/sbt-launcher-packaging-0.13.13/bin/:${PATH}"
RUN java -version
RUN sbt --version
RUN yum install -y epel-release
RUN yum install -y iperf
RUN yum install -y net-tools
RUN yum install -y tcpdump
