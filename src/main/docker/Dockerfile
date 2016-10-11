# https://binfalse.de/2016/05/31/mvn-automagically-create-a-docker-image/

# FROM tomcat:8-jre8
FROM tomcat:9.0.0-jre8
MAINTAINER Joachim Russig

# remove the default tomcat application
# RUN rm -rf /usr/local/tomcat/webapps/ROOT /usr/local/tomcat/webapps/ROOT.war
# COPY heatstressrouting.war /usr/local/tomcat/webapps/ROOT.war

COPY heatstressrouting.war /usr/local/tomcat/webapps/heatstressrouting.war