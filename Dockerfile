FROM amd64/openjdk:12

ENV SBT_VERSION 1.1.4


# Install dos2unix
RUN yum install dos2unix -y

# Install SBT
RUN yum install -y https://dl.bintray.com/sbt/rpm/sbt-$SBT_VERSION.rpm


# install base dependencies and plugins as defind in project config files
# todo - install to an anon directory if necessary
WORKDIR /appsrc
COPY build.sbt /appsrc
COPY project/plugins.sbt /appsrc/project/
RUN dos2unix /appsrc/build.sbt \
	&& dos2unix /appsrc/project/plugins.sbt
RUN sbt update
