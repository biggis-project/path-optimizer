#' Checks if point is within the bounding box bbox.
#'
#' @param point a named list with (lat, lng) values
#' @param bbox a bounding box with (lat_min, lat_max, lng_min, lng_max) values
#'
withinBBox <- function(point, bbox) {
  ret <-   point$lat > bbox$lat_min &&
    point$lat < bbox$lat_max &&
    point$lng > bbox$lng_min &&
    point$lng < bbox$lng_max
  return(ret)
}

#' Loads the thermal scans and resamples them to 'max_pixel' if neccesary.
#' The function checks if allready a resampled file exist, e.g.
#' 'raster_morgen_georect_1000000px.tif' and load this instead.
#'
#' @param base_dir directory where the files a located
#' @param morgen_file the thermal scan 'morgen'
#' @param abend_file the thermal scan 'abend'
#' @param max_pixel the maximum number of pixel; can be NULL
#'
#' @return a list of (morgen, abend) of the loaded and resampled raster data
#'
loadRasterData <- function(base_dir = "data/",
                           morgen_file = "thermal-flight-karlsruhe-morning.tif",
                           abend_file = "thermal-flight-karlsruhe-evening.tif",
                           max_pixel = 1000000) {
  # Assamble the filenames with an '_$max_pixel$px' suffix,
  # e.g. 'raster_morgen_georect_1000000px.tif
  if (!is.null(max_pixel)) {
    morgen_file_px <-
      paste0(
        strsplit(morgen_file, "\\.")[[1]][1],
        "_",
        as.integer(max_pixel),
        "px.",
        strsplit(morgen_file, "\\.")[[1]][2]
      )
    abend_file_px <-
      paste0(
        strsplit(abend_file, "\\.")[[1]][1],
        "_",
        as.integer(max_pixel),
        "px.",
        strsplit(abend_file, "\\.")[[1]][2]
      )
    
    # if a resampled file allready exists load this instead of performing a slow resapmling
    if (file.exists(paste0(base_dir, morgen_file_px))) {
      raster_morgen_georect <-
        raster::raster(readGDAL(paste0(base_dir, morgen_file_px)))
    } else {
      raster_morgen_georect <-
        raster::raster(readGDAL(paste0(base_dir, morgen_file)))
      raster_morgen_georect <-
        sampleRegular(raster_morgen_georect, max_pixel, asRaster = T)
    }
    
    # if a resampled file allready exists load this instead of performing a slow resapmling
    if (file.exists(paste0(base_dir, abend_file_px))) {
      raster_abend_georect <-
        raster::raster(readGDAL(paste0(base_dir, abend_file_px)))
    } else {
      raster_abend_georect <-
        raster::raster(readGDAL(paste0(base_dir, abend_file)))
      raster_abend_georect <-
        sampleRegular(raster_abend_georect, max_pixel, asRaster = T)
    }
  }
  
  ret <- list(morgen = raster_morgen_georect,
              abend = raster_abend_georect)
  return(ret)
}

#' Parses the provide string and returns the geolocation as list \code{(lat, lng)} or \code{NULL}
#' the input can be either
#'   - a geolocation as latitude-longitude pair seperated by a comma, e.g. "49.0118083,8.4251357"
#'   - a address or an place which is geocoded using ggmap::geocode
#'
#' @param loc the location string to be parsed
#' @param source the source to be used for geocoding, either ggmap (google maps) or photon
#'
#' @return the parsed location as list of (lat, lng)
parseLocation <- function(loc, source = c("ggmap", "photon")) {
  source <- match.arg(source)
  loc <- trimws(loc)
  # TODO improve numeric regex
  if (all(grepl("^-?[0-9]*\\.[0-9]*,\\s*-?[0-9]*\\.[0-9]*$", loc))) {
    vals <- as.numeric(unlist(strsplit(loc, ",")))
    return(list(lat = vals[1], lng = vals[2]))
  }
  
  res <- list()
  flog.debug("loc = %s", loc)
  if (source == "ggmap") {
    tmp <- ggmap::geocode(loc)
    if (!anyNA(tmp) && !is.null(tmp$lat) && !is.null(tmp$lon)) {
      res <- list(lat = tmp$lat, lng = tmp$lon)
    } else {
      res <- NULL
    }
  } else {
    if (!require(photon)) {
      require(devtools)
      devtools::install_github(repo = 'rCarto/photon')
    }
    
    tmp <- photon::geocode(loc, limit = 1)
    flog.debug("photon result: ", tmp, capture = T)
    if (nrow(tmp) > 0) {
      res <- list(lat = tmp[1, ]$lat, lng = tmp[1, ]$lon)
    } else {
      res <- NULL
    }
  }
  # flog.debug("res = ", res, capture = T)
  return(res)
}

#' Parses the location provided as string and checks if it is within the bounding box.
#' Creates a shinyBS alert if the provided location is not valid.
#' 
#' @param str the locaiton to check
#' @param name the name to be displaed in the error message, e.g. 'start' or 'destination'
#' @param bbox the bounding box
#' @param shiny_session the shiny session were ther error message should be displayed
#' @param anchorId the anchorId of the shinyBS::bsAlert anchor
#' @param alertId the alertId to use for the alert
#' 
#' @retun the parsed coordinat as list of (lat, lng) or NULL if str is not valid  
checkLocation <-
  function(str,
           name,
           bbox,
           shiny_session,
           anchorId,
           alertId) {
    closeAlert(shiny_session, alertId = alertId)
    flog.debug("checkLocation: str = %s, nchar(str) = %s", str, nchar(str))
    if (is.null(str) || nchar(str) == 0) {
      createAlert(
        session = shiny_session,
        anchorId = anchorId,
        alertId = alertId,
        content = paste0("please specifie a ", name),
        style = "danger",
        append = F
      )
      return(NULL)
    }
    tmp <- parseLocation(str)
    flog.debug("tmp = ", tmp, capture = T)
    if (is.null(tmp) || is.na(tmp)) {
      createAlert(
        session = shiny_session,
        anchorId = anchorId,
        alertId = alertId,
        title = "Oops!",
        content = paste0("could not find '", htmlEscape(str), "'"),
        style = "danger",
        append = F
      )
      return(NULL)
    }
    flog.debug("withinBBox() = ", withinBBox(tmp, bbox))
    if (!withinBBox(tmp, bbox)) {
      createAlert(
        session = shiny_session,
        anchorId = anchorId,
        alertId = alertId,
        title = "Oops!",
        content = paste0(name, " '", htmlEscape(str), "' is not within the bounding box"),
        style = "danger",
        append = F
      )
      return(NULL)
    }
    return(tmp)
  }


#' Collapse a list of (lat, lng) to a string 'lat,lng' 
#' as required by the backend server, e.g. 
#'  list(lat = 49.0118083, lng = 8.4251357) => '49.0118083,8.4251357'
#'
asRequestPoint <- function(loc) {
  stopifnot(c("lat", "lng") %in% names(loc))
  if (!is.null(loc)) {
    return(paste(loc$lat, loc$lng, sep = ","))
  } else {
    return(NA_character_)
  }
}

#' Formats a POSIx date and a POSIX time object as required by the back-end server,
#' e.g. 2015-08-31T10:00:00
asRequestDateTime <- function(date, time) {
  return(paste0(format(date, "%Y-%m-%d"), "T", format(time, "%T")))
}

# http://stackoverflow.com/questions/8197559/emulate-ggplot2-default-color-palette/8197703#8197703
gg_color_hue <- function(n) {
  hues = seq(15, 375, length = n + 1)
  hcl(h = hues, l = 65, c = 100)[1:n]
}

#' Collpase a list of key value pairs to a URL query string
#' Example: list(a = 1, b = "str") => 'a=1&b=str'
collapseArgs <- function(l) {
  if (!is.list(l)) {
    stop("'l' must be a list")
  }
  res <- character()
  for (i in 1:length(l)) {
    res <- rbind(res, paste(names(l[i]), "=", l[i], sep = ""))
  }
  return(paste(res, collapse = "&"))
}

# Creates a factory for improved error and warning handling
# Source: http://stackoverflow.com/questions/4948361/how-do-i-save-warnings-and-errors-as-output-from-a-function/4952908#4952908
tryCatchFactory <- function(fun) {
  function(...) {
    warn <- err <- NULL
    res <- withCallingHandlers(
      tryCatch(
        fun(...),
        error = function(e) {
          err <<- conditionMessage(e)
          NULL
        }
      ),
      warning = function(w) {
        warn <<- append(warn, conditionMessage(w))
        invokeRestart("muffleWarning")
      }
    )
    list(res = res,
         warn = warn,
         err = err)
  }
}

#' Formats a duration in seconds, using the following format: HH:MM:SS
formatDurationSecs <- function(duration) {
  stopifnot(is.numeric(duration))
  seconds <- abs(duration)
  h <- as.integer(seconds / 3600)
  m <- as.integer((seconds %% 3600) / 60)
  s <- as.integer(seconds %% 60)
  ret <- sprintf("%d:%02d:%02d", h, m, s)
  if (any(duration <  0))
    ret <- paste0("-", ret)
  return(ret)
}

#' Formats a duration in milli seconds, using the following format: HH:MM:SS.mmm
formatDurationMillis <- function(duration, print.millis = TRUE) {
  stopifnot(is.numeric(duration))
  seconds <- abs(duration / 1000)
  h <- as.integer(seconds / 3600)
  m <- as.integer((seconds %% 3600) / 60)
  s <- as.integer(seconds %% 60)
  if (print.millis == TRUE)
    ret <- sprintf("%d:%02d:%02d.%03d", h, m, s, duration %% 1000)
  else
    ret <- sprintf("%d:%02d:%02d", h, m, s)
  if (any(duration <  0))
    ret <- paste0("-", ret)
  return(ret)
}