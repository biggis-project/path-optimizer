# Based on https://github.com/flaviobarros/shiny-wordcloud/blob/master/Dockerfile

# https://hub.docker.com/r/rocker/shiny/~/dockerfile/
FROM rocker/shiny
MAINTAINER Joachim Russig

# host and port of the back-end server
ARG BACKEND_HOST=localhost
ARG BACKEND_PORT=8080
ARG ENABLE_RASTER_OVERLAY=false

ENV BACKEND_HOST=$BACKEND_HOST
ENV BACKEND_PORT=$BACKEND_PORT
ENV ENABLE_RASTER_OVERLAY=$ENABLE_RASTER_OVERLAY

# Fixed Issue #6
RUN apt-get update; exit 0

RUN apt-get -y install \
	curl \
	libssl-dev \ 
	openssl \
	libgdal-dev \
	libproj-dev \
	libssh2-1-dev \ 
	gdal-bin
	
# install the required r packages; the leaflet package is being installed from github 
# because the latest version 1.0.1 on CRAN  doesn't include all required featues,
# e.g. labels for marker
RUN R -e "install.packages(c('futile.logger', 'reshape2', 'data.table', \
             'prettyunits', 'lubridate', 'httr', 'jsonlite', 'XML', 'RColorBrewer', \
             'viridis', 'ggmap', 'sp', 'raster', 'rgdal', 'rgeos', 'spdep', \
             'devtools', 'shinyBS', 'shinyTime', 'shinyjs'), repos='http://cran.rstudio.com/'); \
          library(devtools); \
          devtools::install_github('rstudio/leaflet');"

# remove the shiny default app
RUN rm -r /srv/shiny-server/*

# copy the shiny app in the shiny server directory
COPY /HeatStressRouting-Frontend/ /srv/shiny-server/
# copy the shiny server configuration and the start up script
COPY shiny-server.conf /etc/shiny-server/shiny-server.conf
COPY shiny-server.sh /usr/bin/shiny-server.sh
# make shiny-server.sh executable
RUN chmod +x /usr/bin/shiny-server.sh 

EXPOSE 80

CMD ["/usr/bin/shiny-server.sh"]