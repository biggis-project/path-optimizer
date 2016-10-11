# About the project

The project contains the prototype of a web application that provides to services:

* **Routing:** a service that finds a route with a minimal heat stress between two points at a given time.
* **Optimal time:** a service that finds an optimal point in time, i.e. a point in time with a minimal heat stress, for every place in a given radius that fulfill a criterion (e.g. is a supermarket). Additional for every place the optimal route between the start point and the place is computed.  

# Prerequisites

- [docker](https://docs.docker.com/engine/installation/)
- [docker-compose](https://docs.docker.com/compose/install/)
- TODO ...

# Try it out

``` sh
$ git clone git://github.com/biggis-project/path-optimizer.git
$ cd path-optimizer
$ ./preprocess-data.sh
$ docker-compose build
$ docker-compose up
```

# Contact

If there are any questions please feel free and send a mail to: <joachim.russig@gmx.de>.
