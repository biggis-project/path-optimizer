source("utils.R")
source("webapi.R")

# The display option 'Show Raster Overlay' can be enabled by setting the 
# enviornment variable "ENABLE_RASTER_OVERLAY" to 'true'
enableRasterOverlay <- as.logical(Sys.getenv("ENABLE_RASTER_OVERLAY", unset = FALSE))

#' Adds an on click listner to a specified layer and 
#' triggers an input message if the layer is clicked.
#'  
#' @param map the \code{leaflet} or \code{leafletProxy} object
#' @param category the category the on click listner should be applied to, e.g. shape
#' @param layerId the layer the on click listner should be attached to
#' @param inputId the id of the input triggered on click 
addOnClickListner <- function(map, category, layerId, inputId) {
  flog.debug(
    "addOnClickListner: category = %s, layerId = %s, inputId = %s",
    category,
    layerId,
    inputId,
    data = list()
  )
  # invokes the addOnClickListner JavaScript method defined in www/message-handler.js
  leaflet::invokeMethod(map,
                        data,
                        "addOnClickListner",
                        category,
                        layerId,
                        inputId)
}


#' Wraps an element in a simple \code{div} inline-block.
#' 
#' @param element the element that should be wrapped in the inline block
#' @param width the width of the div element
#' @param class the class of the div element
inlineBlock <- function(element, width = NULL, class = NULL) {
  div(
    style = "display: inline-block;",
    style = if (!is.null(width))
      paste0("width: ", validateCssUnit(width), ";"),
    class = if (!is.null(class)) class,
    element
  )
}


#' An \code{actionButton} displayed inline with an other input widget, e.g. an \code{textInput}.
#' 
#' @param inputId the \code{inputId} of the \code{actionButton}
#' @param label the label
#' @param icon the icon
#' @param width the width of the \code{actionButton}
#' 
#' @seealso shiny::actionButton
#' @note http://stackoverflow.com/questions/20637248/shiny-4-small-textinput-boxes-side-by-side/21132918#21132918
inlineActionButton <-
  function(inputId,
           label,
           icon = NULL,
           width = NULL,
           ...) {
    div(
      # A hack to make shure, that the button has the same position as an text input placed side by side
      # and to avoid the differen handling of 'vertical-align: middle' in firefox and chrome
      # TODO: find a better solution
      class = "inline-action-button-container",
      style = "display:inline-block",
      actionButton(
        inputId = inputId,
        label = label,
        icon = icon,
        width = width,
        ...
      ))
  }

#' An timeInput widget to be placed inline.
#' @param inputId the input id
#' @param label the label
#' @param value the initial value
#' @param seconds display a input field for seconds?
#' @param class the html class to be applied on the element
#' @param width the width of the input filed, or NULL
#' @seealso shinyTime::timeInput()
inlineTimeInput <- function(inputId,
                            label,
                            value = NULL,
                            seconds = TRUE,
                            class = "form-control",
                            width = NULL) {
  time_input <- timeInput(
    inputId = inputId,
    label = label,
    value = value,
    seconds = seconds
  )
  # Add the class 'form-control', so that form-contorl styles are applied 
  # to the time input fields
  # TODO: finde a better soloution
  for (i in seq_along(time_input[[2]]$children[[2]]$children)) {
    if (any(time_input[[2]]$children[[2]]$children[[i]]$name == "input")) {
      time_input[[2]]$children[[2]]$children[[i]]$attribs$class <- class
    }
  }
  # return(inlineBlock(time_input, width = width, class = "inline-time-input-container"))
  return(time_input)
}

#' Creates a html table to be showed in the route or marker pop ups.
#'
#' @param vals a named list of key-value-pairs to be displayed in the pop up
#' @param class a class to be applied to the table tag
#' @param digits the number of digits to be displayed, e.g. 5.34
#' @param key.sep seperate used between key and value, e.g. ':'
#' @param fun a optional callback applied to each key-value-pair; it must return a htmltools::tags$tr with to htmltools::tags$td
popupTable <-
  function(vals,
           class = "marker-popup",
           digits = 2,
           key.sep = ":",
           fun = NULL) {
    htmltools::withTags(table(class = class,
                              Map(function(key, val) {
                                if (!is.null(fun) && is.function(fun)) {
                                  res <- fun(key, val)
                                  res
                                } else {
                                  if (is.numeric(val) && !is.null(digits))
                                    val <- round(val, digits = digits)
                                  tr(td(paste0(key, key.sep)),
                                     td(val))
                                }
                              }, names(vals), vals)))
  }

# creates the popup label displayed, when the user clicks on a route
popupTableRouting <- function(dt, cols) {
  stopifnot(is.data.table(dt))
  dt <- dt[, names(dt) %in% cols, with = F]
  dt <- dt[, cols, with = F]
  popupTable(
    dt[, names(dt) %in% cols, with = F],
    fun = function(key, val) {
      if (key == "distance") {
        val <- paste0(round(val, digits = 2), " m")
      } else if (key == "diff_distance") {
        if (val < 0)
          val <- span(class = "good", paste0(round(val, digits = 2), " m"))
        else if (val > 0)
          val <- span(class = "poor", paste0("+", round(val, digits = 2), " m"))
        else
          val <- val
      } else if (key == "diff_duration") {
       if (startsWith(val, "-"))
         val <- span(class = "good", val)
        else if (!any(grepl("0:00:00", val)))
          val <- span(class = "poor", paste0("+", val))
        else
          val <- val
      } else if (key == "rel_distance") {
        val <- -round(val * 100, 2)
        if (val < 0)
          val <- span(class = "good", paste0(val, "%"))
        else if (val > 0)
          val <- span(class = "poor", paste0("+", val, "%"))
        else
          val <- paste0(val, "%")
      } else if (grepl(".per.distance", key)) {
        key <- sub("route_weight\\.([[:alpha:]]+)\\.per\\.distance", "\\1 / distance", key)
        val <- paste0(round(val, digits = 2), "°C")
      } else if (grepl("rel.route_weight", key)) {
        key <- sub("rel.route_weight\\.([[:alpha:]]+)", "difference heat stress (\\1)", key)
        val <- -round(val * 100, 2)
        if (val < 0)
          val <- span(class = "good", paste0(val, "%"))
        else if (val > 0)
          val <- span(class = "poor", paste0("+", val, "%"))
        else
          val <- paste0(val, "%")
      } else if (lubridate::is.duration(val)){
        val <- formatDurationSecs(as.integer(val)) 
      }
      if (!is.null(val)) {
        return(tags$tr(
          tags$td(paste0(key, ":")),
          tags$td(val)
        ))
      } else {
        return(NULL)
      }
    }
  )
}

popupTableOptimalTime <- function(dt, cols) {
  stopifnot(is.data.table(dt))
  dt <- dt[, names(dt) %in% cols, with = F]
  dt <- dt[, cols, with = F]
  popupTable(
    dt,
    fun = function(key, val) {
      if (key == "distance") {
        val <- paste0(round(val, digits = 2), " m")
      } else if (key == "duration") {
        # val <- lubridate::as.duration(val / 1000)
        val <- formatDurationMillis(val, print.millis = F)
      } else if (key == "optimal_time") {
        val <- format(strptime(val, format = "%Y-%m-%dT%H:%M"), format = "%H:%M")
      } else if (key == "optimal_value") {
        val <- round(val, digits = 2)
      }
      if (!is.null(val)) {
        return(tags$tr(
          tags$td(paste0(key, ":")),
          tags$td(val)
        ))
      } else {
        return(NULL)
      }
    }
  )
}

