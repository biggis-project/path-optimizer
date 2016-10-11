library(httr)
library(jsonlite)
library(lubridate)
library(futile.logger)

BACKEND_HOST <-
  ifelse(!is.na(Sys.getenv("BACKEND_HOST", unset = NA)),
         Sys.getenv("BACKEND_HOST"),
         "localhost")
BACKEND_PORT <-
  ifelse(!is.na(Sys.getenv("BACKEND_PORT", unset = NA)),
         Sys.getenv("BACKEND_PORT"),
         "8080")

BACKEND_URL <- paste0("http://", BACKEND_HOST, ":", BACKEND_PORT, "/heatstressrouting/api/v1/")
BACKEND_INFO_URL <- paste0(BACKEND_URL, "info")
BACKEND_ROUTING_URL <- paste0(BACKEND_URL, "routing")
BACKEND_OPTIMALTIME_URL <- paste0(BACKEND_URL, "optimaltime")

#' Requestes the supported bounding box from the backend server.
#' 
#' @param baseurl url where the data can requested from the server
#' @return list of (lat_min, lng_min, lat_max, lng_max) supported by the backend server
getBBox <- function(baseurl = "http://localhost:8080/heatstressrouting/api/v1/info") {
  rsp <- httr::GET(url = baseurl)
  json <- jsonlite::fromJSON(content(rsp, as = "text"))
  return(
    list(
      lat_min = json$bbox[1],
      lng_min = json$bbox[2],
      lat_max = json$bbox[3],
      lng_max = json$bbox[4]
    )
  )
}

#' Requestes the supported time range from the backend server.
#' 
#' @param baseurl url where the data can requested from the server
#' @return the supported time range as a list of posix date time values
getTimeRange <- function(baseurl = "http://localhost:8080/heatstressrouting/api/v1/info") {
  rsp <- httr::GET(url = baseurl)
  json <- jsonlite::fromJSON(content(rsp, as = "text"))
  return(
    list(
      from = strptime(json$time_range$from, format = "%Y-%m-%dT%H:%M"),
      to = strptime(json$time_range$to, format = "%Y-%m-%dT%H:%M")
    )
  )
}

#' Requestes a route from the backend server.
#'
#' @param start the start point, a list of ("lat", "lng"); note, that the values must be named accordingly
#' @param destination the destnation point, a list of ("lat", "lng"); note, that the values must be named accordingly
#' @param time the requested time, either as a character string of the form '2015-08-31T10:00' or as a instance of \code{POSIXt}
#' @param weighting a list of the weightings, that should be used
#' @param baseurl the url of the routing service
getRoute <- function(start,
                     destination,
                     time,
                     weighting = c("shortest", "heatindex", "temperature", "heatindexweighted"),
                     baseurl = "http://localhost:8080/heatstressrouting/api/v1/routing") {
  weighting <- match.arg(weighting, several.ok = T)
  
  stopifnot(c("lat", "lng") %in% names(start))
  stopifnot(c("lat", "lng") %in% names(destination))
  
  if (is.POSIXt(time)) {
    time <- format(time, format = "%Y-%m-%dT%H:%M")
  }
  
  start <- asRequestPoint(start)
  destination <- asRequestPoint(destination)
  
  # example request: "http://localhost:8080/heatstressrouting/api/v1/routing?start=49.0118083,8.4251357&destination=49.0126868,8.4065707&time=2015-08-31T10:00:00")
  args <-
    list(
      start = start,
      destination = destination,
      time = time,
      weighting = paste(weighting, collapse = ",")
    )
  query <- paste0(baseurl, "?", collapseArgs(args))
  flog.info("Query route (url = %s", query)
  rsp <- httr::GET(url = baseurl, query = args)
  # flog.debug("rsp = ", rsp, capture = T)
  json <- jsonlite::fromJSON(content(rsp, as = "text"))
  
  df <- data.frame()
  if (json$status == "OK") {
    for (w in names(json$results)) {
      tmp <-
        data.frame(lat = json$results[[w]]$path[, 1],
                   lng = json$results[[w]]$path[, 2])
      tmp$weighting <- w
      df <- rbind(df, tmp)
    }
  }
  
  res <- list(
    status = json$status,
    status_code = json$status_code,
    json = json,
    routing_paths = df
  )
  # flog.debug("res = ", res, capture = T)
  return(res)
}

#' Requests an optimal point in time from the backend server.
#' 
#' @param start the start point, a list of ("lat", "lng"); note, that the values must be named accordingly
#' @param time the requested time, either as a character string of the form '2015-08-31T10:00' or as a instance of \code{POSIXt}
#' @param place_type the type of the place, note that only a restricted number of values is accepted by the server
#' @param max_distance the maximum direct distance between the start and the place in meter
#' @param max_results the maximum number of results to consider
#' @param time_buffer the time needed at the place in minuites
#' @param earliest_time the earliest point in time accepted
#' @param latest_time the latest point in time accepted
#' @param baseurl the url of the optimal time service
getOptimalTime <- function(start,
                           time,
                           place_type,
                           max_distance = 1000,
                           max_results = 5,
                           time_buffer = 15,
                           earliest_time = NULL,
                           latest_time = NULL,
                           baseurl = "http://localhost:8080/heatstressrouting/api/v1/optimaltime") {
  
  stopifnot(c("lat", "lng") %in% names(start))
  
  start <- asRequestPoint(start)
  
  if (is.POSIXt(time)) {
    time <- format(time, format = "%Y-%m-%dT%H:%M")
  }
  if (is.POSIXt(earliest_time)) {
    earliest_time <- format(earliest_time, format = "%Y-%m-%dT%H:%M")
  }
  if (is.POSIXt(latest_time)) {
    latest_time <- format(latest_time, format = "%Y-%m-%dT%H:%M")
  }
  
  # example request: "http://localhost:8080/heatstressrouting/api/v1/optimaltime?start=49.0118083,8.4251357&time=2015-08-31T10:00:00&place_type=supermarket"
  args <-
    list(
      start = start,
      time = time,
      place_type = paste0(place_type, collapse = ","),
      max_distance = max_distance,
      max_results = max_results,
      time_buffer = time_buffer,
      earliest_time = earliest_time,
      latest_time = latest_time
    )
  query <- paste0(baseurl, "?", collapseArgs(args))
  flog.info("Query optimal time (url = %s", query)
  rsp <- httr::GET(url = baseurl, query = args)
  # flog.debug("rsp = ", rsp, capture = T)
  
  flog.debug("getOptimalTime: status_code = %s, url = %s", rsp$status_code, rsp$url)
  
  # TODO: handle JSON parsing errors
  json <- jsonlite::fromJSON(content(rsp, as = "text"))
  
  res <- list(
    status = json$status,
    status_code = json$status_code,
    json = json
  )
  # flog.debug("res = ", res, capture = T)
  return(res)
}
