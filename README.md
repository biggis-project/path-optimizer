[![Build Status](https://travis-ci.org/biggis-project/path-optimizer.svg?branch=master)][Travis]
[Travis]: https://travis-ci.org/biggis-project/path-optimizer

# About the project

The project contains the prototype of a web application that provides to services:
- **Routing:** a service that finds a route with a minimal heat stress between
  two points at a given time.
- **Optimal time:** a service that finds an optimal point in time, i.e. a point
  in time with a minimal heat stress, for every place in a given radius that
  fulfill a criterion (e.g. is a supermarket). Additional for every place the
  optimal route between the start point and the place is computed.  


# Running DEMO
A running docker-based instance is running on our server:
http://ipe-koi09.fzi.de:8000


# Prerequisites
- [mvn (Maven)](https://maven.apache.org/install.html)
  - `sudo apt install maven`
- [docker](https://docs.docker.com/engine/installation/)
- [docker-compose](https://docs.docker.com/compose/install/)
- [R](https://cran.r-project.org/doc/FAQ/R-FAQ.html#How-can-R-be-installed_003f)
- [gdal](https://trac.osgeo.org/gdal/wiki/DownloadingGdalBinaries)
- [osmosis](https://wiki.openstreetmap.org/wiki/Osmosis#How_to_install)
  - `sudo apt install osmosis`
- Windows: e.g. [cygwin](https://cygwin.com/install.html) to execute sh scripts
- TODO ...


# Try it out
```sh
$ git clone https://github.com/biggis-project/path-optimizer.git
$ cd path-optimizer
$ mvn clean install
$ docker-compose up
```
Now go to `http://localhost:8000/`

# Contact
If there are any questions please feel free and send a mail to: <joachim.russig@gmx.de>.
