FROM centos:7
ENV	HOME /root
ENV	LANG en_US.UTF-8
ENV	LC_ALL en_US.UTF-8

RUN yum install yum upgrade -y; yum update -y;  yum clean all

RUN yum install -y strace procps tree vim git curl wget gnuplot unzip net-tools
RUN yum groupinstall -y 'Development Tools'
RUN yum install -y openssl-devel

ENV JDK_VERSION 8u11
ENV JDK_BUILD_VERSION b13
RUN mkdir -p /usr/local
RUN curl -LO "http://download.oracle.com/otn-pub/java/jdk/8u121-b13/e9e7ea248e2c4826b92b3f075a80e441/jdk-8u121-linux-x64.tar.gz" -H 'Cookie: oraclelicense=accept-securebackup-cookie' && tar -xvz -f jdk-8u121-linux-x64.tar.gz -C /usr/local
ENV JAVA_HOME /jdk1.8.0_121/bin/
RUN ls /usr/local/

RUN curl -LO "https://dl.bintray.com/sbt/native-packages/sbt/0.13.15/sbt-0.13.15.tgz" && tar -xzv  -f sbt-0.13.15.tgz -C /usr/local
ENV PATH="/usr/local/jdk1.8.0_121/bin/:/usr/local/sbt/bin/:${PATH}"
RUN java -version
RUN sbt --version
