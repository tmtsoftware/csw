FROM centos

# Env variables
ENV SCALA_VERSION 2.12.7
ENV SBT_VERSION 1.2.6
ENV JAVA_VERSION 1.8.0

RUN yum -y update && yum clean all

# installing openjdk 8
RUN yum install -y java-$JAVA_VERSION-openjdk java-$JAVA_VERSION-openjdk-devel

# install wget
RUN yum install -y wget

# installing scala and sbt
RUN yum install -y https://downloads.lightbend.com/scala/$SCALA_VERSION/scala-$SCALA_VERSION.rpm
RUN yum install -y https://dl.bintray.com/sbt/rpm/sbt-$SBT_VERSION.rpm

# Make sure you are building this image from csw root directory
ADD . /csw

# go inside csw
WORKDIR /csw

RUN sbt compile

ENTRYPOINT ["sbt", "csw-time-client/test"]
