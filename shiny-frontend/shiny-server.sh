#!/bin/sh

# Based on https://github.com/flaviobarros/shiny-wordcloud/blob/master/shiny-server.sh  

# Make sure the directory for individual app logs exists
mkdir -p /var/log/shiny-server
chown shiny.shiny /var/log/shiny-server

# Add the enviornment variables to the user 'shiny', 
# so that those can be accessed from the R process
# See also http://docs.rstudio.com/shiny-server/#run_as
echo export BACKEND_HOST=$BACKEND_HOST >> /home/shiny/.bash_profile
echo export BACKEND_PORT=$BACKEND_PORT >> /home/shiny/.bash_profile
echo export ENABLE_RASTER_OVERLAY=$ENABLE_RASTER_OVERLAY >> /home/shiny/.bash_profile

# Starting the shiny server
exec shiny-server >> /var/log/shiny-server.log 2>&1

# echo starting shiny server...
# timeout 1m shiny-server >> /var/log/shiny-server.log 2>&1
# echo shiny server exited
# 
# ls -a /var/log/shiny-server
# tail -f -n 100 /var/log/shiny-server/*
# tail -n 50 /var/log/shiny-server.log