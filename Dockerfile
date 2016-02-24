FROM ubuntu:latest
MAINTAINER anthony kitchin

# Install Java.
RUN \
  apt-get install -y software-properties-common && \
  echo oracle-java7-installer shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && \
  add-apt-repository -y ppa:webupd8team/java && \
  add-apt-repository -y ppa:stebbins/handbrake-git-snapshots && \
  apt-get update && \
  apt-get install -y oracle-java7-installer handbrake-cli curl unzip maven && \
  rm -rf /var/lib/apt/lists/* && \
  rm -rf /var/cache/oracle-jdk7-installer

RUN echo "deb http://download.videolan.org/pub/debian/stable/ /" > /etc/apt/sources.list.d/vlc-libdvdcss.list; \
    echo "deb-src http://download.videolan.org/pub/debian/stable/ /" >> /etc/apt/sources.list.d/vlc-libdvdcss.list; \
    wget -O - http://download.videolan.org/pub/debian/videolan-apt.asc | sudo apt-key add -

RUN \
  apt-get update && \
  apt-get install -y \
    libdvdcss2 && \
  rm -rf /var/lib/apt/lists/* && \
  rm -rf /var/cache/oracle-jdk7-installer

RUN curl -L https://github.com/akitchin/docker-handbrake/archive/master.zip > master.zip
RUN unzip master.zip
RUN cd /docker-handbrake-master/nas-runner && mvn install

# Define working directory.
VOLUME /data

# Define commonly used JAVA_HOME variable
ENV JAVA_HOME /usr/lib/jvm/java-7-oracle

CMD ["bash", "-c", "cd /docker-handbrake-master/nas-runner && mvn exec:java -Dexec.mainClass=com.hazmit.nas_runner.App -Dexec.args=\"/usr/bin/HandBrakeCLI /data/INCOMING\""]
