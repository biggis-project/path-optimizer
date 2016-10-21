library(shiny)
# leaftlet version from github required
# devtools::install_github("rstudio/leaflet")
library(leaflet)
library(shinyBS)
library(httr)
library(jsonlite)
library(XML)
library(htmltools)
library(RColorBrewer)
library(viridis)

library(futile.logger)
library(reshape2)
library(data.table)
library(prettyunits)
library(lubridate)

library(ggmap)
library(sp)
library(raster)
library(rgdal)
library(rgeos)
library(spdep)

# The raster data are load once on server start up,
# because resampling and projection can take some time.
raster_data <- loadRasterData()

shinyServer(function(input, output, session) {
  
  # flog.threshold(DEBUG)
  flog.threshold(INFO)
  flog.appender(appender.console())
  # flog.appender(appender.file('/var/log/shiny-server/server.log'))
  
  # The bounding box and the time range is queried 
  # once per session from the back-end server
  bbox <- getBBox(baseurl = BACKEND_INFO_URL)
  time_range <- getTimeRange(baseurl = BACKEND_INFO_URL)
  
  dat <- reactiveValues(
      map_center = list(lng = 8.4251357, lat = 49.0118083),
      bbox = bbox,
      time_range = time_range,
      date_default = time_range$to,
      time_default = strptime("10:00:00", "%T"),
      time_default_earliest = strptime("09:00:00", "%T"),
      time_default_latest = strptime("20:00:00", "%T"),
      routing_start_point = NULL,
      routing_destination_point = NULL,
      routing_select_start_clicked = FALSE,
      routing_select_destination_clicked = FALSE,
      routing_json = NULL, # raw server response
      routing_paths = NULL,
      routing_table = NULL,
      optimaltime_start_point = NULL,
      optimaltime_select_start_clicked = FALSE,
      optimaltime_json = NULL, # raw server response
      optimaltime_table = NULL
    )
  
  #################################################
  #                                               #
  # ROUTING:                                      #  
  #                                               #  
  #################################################
  
  #################################################
  # Input                                         #  
  #################################################
  
  output$routing_date_input <- renderUI({
    dateInput(
      "routing_date",
      "Date:",
      min = as.Date(dat$time_range$from),
      max = as.Date(dat$time_range$to),
      value = as.Date(dat$date_default)
    )
  })


  output$routing_time_input <- renderUI({
    # http://stackoverflow.com/questions/27198515/ui-elements-for-selecting-date-and-time-not-just-date-in-shiny
    # see: global.R
    inlineTimeInput(
      "routing_time",
      "Time:",
      value = dat$time_default,
      seconds = FALSE
    )
  })

  #################################################
  # Output                                        #  
  #################################################
  
  ## draw the initial map
  output$map_routing <- renderLeaflet({
    raster_pal_morgen <- colorNumeric(palette = viridis(100, option = "B"),
                               domain = raster_data[["morgen"]]@data@values,
                               na.color = "#00000000")
    raster_pal_abend <- colorNumeric(palette = viridis(100, option = "B"),
                                      domain = raster_data[["abend"]]@data@values,
                                      na.color = "#00000000")

    leaflet() %>%
      addOnClickListner(category = "shape",
                        layerId = "routing-bbox",
                        inputId = "routing_bbox_click") %>%
      setView(
        lng = dat$map_center$lng,
        lat = dat$map_center$lat,
        zoom = 14
      ) %>%
      addTiles(group = "OpenStreetMap",
               options = providerTileOptions(noWrap = TRUE)) %>%
      addRasterImage(
        raster_data[["morgen"]],
        colors = raster_pal_morgen,
        layerId = "raster_morgen",
        group = "raster_morgen",
        project = FALSE,
        opacity = 0.6
      ) %>%
      addRasterImage(
        raster_data[["abend"]],
        colors = raster_pal_abend,
        layerId = "raster_abend",
        group = "raster_abend",
        project = FALSE,
        opacity = 0.6
      ) %>% hideGroup("raster_morgen") %>% hideGroup("raster_abend")
  })

  output$routing_results_table <- shiny::renderDataTable({
    # computeTable()
    tab <- dat$routing_table
    if (!is.null(dat$routing_table) &&
        all(c("weighting", "distance", "duration") %in% names(tab))) {
      tab <- tab[, .(weighting, distance, duration)]
    } else {
      tab <- NULL
    }
    tab
  })

  #################################################
  # Handle Events                                 #  
  #################################################
  
  observeEvent(input$routing_display_options, {
    map <- leafletProxy("map_routing")
    if ("show_bbox" %in% input$routing_display_options) {
      map %>% showGroup("bbox")
    } else {
      map %>% hideGroup("bbox")
    }

    # show or hide thermal scan
    if ("show_thermalscan" %in% input$routing_display_options) {
      if (input$routing_time < strptime("12:00:00", "%T")) {
        map %>% showGroup("raster_morgen")
      } else {
        map %>% showGroup("raster_abend")
      }
    } else {
      map %>% hideGroup("raster_morgen") %>% hideGroup("raster_abend")
    }

  }, ignoreNULL = FALSE)

  observeEvent(input$routing_weightings, {
    renderRoute()
  }, ignoreNULL = FALSE)
  
  observeEvent(
    c(
      input$routing_start,
      input$routing_destination,
      input$routing_date,
      input$routing_time
    ),
    {
      renderRoute()
    }
  )

  observeEvent(input$routing_select_start, {
    if (dat$routing_select_start_clicked == TRUE) {
      dat$routing_select_start_clicked <- FALSE
    } else {
      dat$routing_select_start_clicked <- TRUE
      dat$routing_select_destination_clicked <- FALSE
    }
    session$sendCustomMessage(
      type = 'routing_select_start',
      message = list(clicked = dat$routing_select_start_clicked)
    )
    session$sendCustomMessage(
      type = 'routing_select_destination',
      message = list(clicked = dat$routing_select_destination_clicked)
    )
  })

  observeEvent(input$routing_select_destination, {
    if (dat$routing_select_destination_clicked == TRUE) {
      dat$routing_select_destination_clicked <- FALSE
    } else {
      dat$routing_select_destination_clicked <- TRUE
      dat$routing_select_start_clicked <- FALSE
    }
    session$sendCustomMessage(
      type = 'routing_select_start',
      message = list(clicked = dat$routing_select_start_clicked)
    )
    session$sendCustomMessage(
      type = 'routing_select_destination',
      message = list(clicked = dat$routing_select_destination_clicked)
    )
  })

  observeEvent(input$map_routing_click, {
    click <- input$map_routing_click
    clat <- click$lat
    clng <- click$lng
    if (dat$routing_select_start_clicked) {
      updateTextInput(session, "routing_start", value = paste0(clat, ",", clng))
    } else if (dat$routing_select_destination_clicked) {
      updateTextInput(session, "routing_destination", value = paste0(clat, ",", clng))
    }
  })

  observeEvent(input$routing_bbox_click, {
    click <- input$routing_bbox_click
    clat <- click$lat
    clng <- click$lng
    if (dat$routing_select_start_clicked) {
      updateTextInput(session, "routing_start", value = paste0(clat, ",", clng))
    } else if (dat$routing_select_destination_clicked) {
      updateTextInput(session, "routing_destination", value = paste0(clat, ",", clng))
    }
  })
  
  #################################################
  # Server request                                #  
  #################################################

  # Request the route from the back-end server
  requestRoute <- reactive({
    
    start_point <- checkLocation(
      input$routing_start,
      name = "start",
      bbox = dat$bbox,
      shiny_session = session,
      anchorId = "routing_alert_start",
      alertId = "routing_alert_start_id"
    )

    destination_point <- checkLocation(
      input$routing_destination,
      name = "destination",
      bbox = dat$bbox,
      shiny_session = session,
      anchorId = "routing_alert_destination",
      alertId = "routing_alert_destination_id"
    )

    # either start or destination contains an error
    if (is.null(start_point) || is.null(destination_point))
      return(NULL)

    flog.debug(
      "input$routing_time = %s, input$routing_date = %s, dat$time_default = %s",
      input$routing_time,
      dat$routing_date,
      dat$time_default
    )
    
    # the time input is not valid
    if (!is.null(input$routing_time) && is.na(input$routing_time))
      return(NULL)
    
    if (is.Date(input$routing_date)) {
      req_date <- input$routing_date
    } else {
      req_date <- dat$date_default
    }
    
    if (is.POSIXlt(input$routing_time)) {
      req_time <- input$routing_time
    } else {
      req_time <- dat$time_default
    }
    req_date_time <- asRequestDateTime(req_date, req_time)
    
    flog.debug("req_date_time = ", req_date_time, capture = T)
    
    if (req_date_time < dat$time_range$from ||
        req_date_time > dat$time_range$to) {
      createAlert(
        session = session,
        anchorId = "routing_alert_datetime",
        alertId = "routing_alert_datetime_id",
        content = paste0(
          "date time'", req_date_time,
          "' is not within the supported time range: ",
          dat$time_range$from ,
          " to ",
          dat$time_range$to
        ),
        style = "danger",
        append = F
      )
      return(NULL)
    } else {
      closeAlert(session, "routing_alert_datetime_id")
    }

    dat$routing_start_point <- start_point
    dat$routing_destination_point <- destination_point

    res <- getRoute(
      baseurl = BACKEND_ROUTING_URL,
      start = start_point,
      destination = destination_point,
      time = req_date_time,
      weighting = c("shortest", input$routing_weightings)
    )

    if (res$status != "OK") {
      createAlert(session = session,
                  anchorId = "routing_alerts",
                  alertId = "routing_alerts_id",
                  title = "Oops! An Internal Server Error Occourd",
                  content = res$json$messages,
                  style = "danger",
                  append = F)
      return(NULL)
    }

    closeAlert(session, "routing_alerts_id")

    dat$routing_json <- res$json

    res
  })

  #################################################
  # Render map                                    #  
  #################################################
  
  # Update and render the map
  renderRoute <- reactive({

    withProgress(message = "Requesting routes...", {
      rsp <- requestRoute()
      incProgress(.35, "Calculating result table...")
      tab <- computeTable()

      if (!is.null(rsp)) {
        incProgress(.05, "Drawing map...")

        routing_paths <- rsp$routing_paths
        dat$routing_paths <- routing_paths

        routing_paths$weighting <- as.factor(routing_paths$weighting)
        groups <- as.character(unique(routing_paths$weighting))

        factpal <-
          colorFactor(rev(gg_color_hue(length(
            unique(routing_paths$weighting)
          ))), routing_paths$weighting)

        # coordinates of the start and destination marker
        start_dest_coord <-
          as.data.frame(rbind(c(name = "Start", head(routing_paths[, c("lat", "lng")], n = 1)),
                              c(name = "Destination", tail(routing_paths[, c("lat", "lng")], n = 1))))

        incProgress(.05, "Drawing map...", detail = "clearing map")

        map <- leafletProxy("map_routing", data = routing_paths) %>%
          clearMarkers() %>% clearShapes() %>% clearControls()

        incProgress(.05, "Drawing map...", detail = "add bounding box")
        # Add the bounding box
        map %>% addRectangles(
          lng1 = dat$bbox$lng_min,
          lat1 = dat$bbox$lat_min,
          lng2 = dat$bbox$lng_max,
          lat2 = dat$bbox$lat_max,
          layerId = "routing-bbox",
          group = "bbox",
          options = pathOptions(className = "routing-bbox")
        )
        if ("show_bbox" %in% input$routing_display_options) {
          map %>% showGroup("bbox")
        } else {
          map %>% hideGroup("bbox")
        }

        incProgress(.05, "Drawing map...", detail = "show/hide thermal scan")
        # Add the thermal scan
        if (is.null(input$routing_time)) {
          routing_time <- dat$time_default
        } else {
          routing_time <- input$routing_time
        }
        # Show the thermal scan
        if ("show_thermalscan" %in% input$routing_display_options) {
          if (routing_time < strptime("12:00:00", "%T")) {
            map %>% showGroup("raster_morgen")
          } else {
            map %>% showGroup("raster_abend")
          }
        } else {
          map %>% hideGroup("raster_morgen") %>% hideGroup("raster_abend")
        }

        incProgress(.05, "Drawing map...", detail = "drawing start and destination marker")
        # Add the start and destination marker
        for (marker_name in start_dest_coord$name) {
          map %>% addMarkers(
            lng = as.numeric(start_dest_coord[start_dest_coord$name == marker_name,]$lng),
            lat = as.numeric(start_dest_coord[start_dest_coord$name == marker_name,]$lat),
            popup = as.character(tags$b(marker_name))
          )
        }

        incProgress(.05, "Drawing map...", detail = "drawing the pathes")
        # add the paths
        for (w in unique(routing_paths$weighting)) {
          d_path <- subset(routing_paths, weighting == w)
          d_tab <- tab[weighting == w, ]

          # cols to include in the popup
          cols <- c("weighting", "distance", "duration", "diff_distance", "diff_duration", "rel_distance")
          cols2 <- unlist(lapply(setdiff(unique(routing_paths$weighting), c("shortest")), function(x) {
            paste0("route_weight.", x, ".per.distance")
          }))
          cols3 <- unlist(lapply(setdiff(unique(routing_paths$weighting), c("shortest")), function(x) {
            paste0("rel.route_weight.", x)
          }))
          cols <- c(cols, cols2, cols3)
          # create the popup displayed, when clicked on the line
          popup_route_tmp <- popupTableRouting(d_tab, cols)
          popup_route <- as.character(popup_route_tmp)
          # flog.debug("popup_route = ", popup_route, capture = T)
          map %>%
            addPolylines(
              data = d_path,
              lat = ~ lat,
              lng = ~ lng,
              group = w,
              color = ~ factpal(weighting),
              weight = 5,
              popup = popup_route,
              opacity = 0.5
            )
        }
        incProgress(.25, "Drawing map...", detail = "add legend")
        map %>%
          addLegend(
            "topright",
            pal = factpal,
            values = ~ weighting,
            title = "Routes",
            opacity = 1
          ) %>%
          addLayersControl(overlayGroups = ~ weighting) %>%
          # TODO fit bounds only, if a point of the path is not within the bounds
          fitBounds(
            lng1 = max(routing_paths$lng),
            lat1 = max(routing_paths$lat),
            lng2 = min(routing_paths$lng),
            lat2 = min(routing_paths$lat)
          )
      }
    })

  })

  # compute the output table
  computeTable <- reactive({

    if (length(dat$routing_json) == 0)
      return(data.frame())

    res_tab <- data.frame()
    for (w in names(dat$routing_json$results)) {
      tmp <- as.data.frame(dat$routing_json$results[[w]]$route_weights)
      colnames(tmp) <- paste("route_weight", colnames(tmp), sep = ".")
      res_tab <-
        rbind(res_tab, cbind(
          weighting = w,
          distance = dat$routing_json$results[[w]]$distance,
          duration_ms = dat$routing_json$results[[w]]$duration,
          duration = as.duration(dat$routing_json$results[[w]]$duration / 1000),
          tmp
        ))
    }
    # flog.info("res_tab = ", res_tab, capture = T)

    res_tab <- as.data.table(res_tab)
    # browser()
    distance_shortest <- res_tab[weighting == "shortest", ]$distance
    duration_shortest <- res_tab[weighting == "shortest", ]$duration
    res_tab[, diff_distance := distance - distance_shortest]
    res_tab[, diff_duration := as.integer(duration - duration_shortest)]

    res_tab[, rel_distance := 1 - (distance / distance_shortest)]
    res_tab[, rel_duration := 1 - (duration / duration_shortest)]

    for (w in names(res_tab)[grep("^route_weight\\..+$", names(res_tab))]) {
      val_shortest <- res_tab[weighting == "shortest", w, with = F]
      res_tab[, (paste0("diff.", w)) := get(w) - as.numeric(val_shortest)]
      res_tab[, (paste0("rel.", w)) := 1 - (get(w) / as.numeric(val_shortest))]
      res_tab[, (paste0(w, ".per.distance")) := get(w) / distance]
    }

    res_tab[, duration := formatDurationSecs(as.integer(duration))]
    res_tab[, diff_duration := formatDurationSecs(as.integer(diff_duration))]
    dat$routing_table <- as.data.table(res_tab)
    res_tab
  })

  #################################################
  #                                               #
  # OPTIMAL TIME                                  #  
  #                                               #  
  #################################################

  #################################################
  # Input                                         #  
  #################################################
  
  output$optimaltime_date_input <- renderUI({
    dateInput(
      "optimaltime_date",
      "Date:",
      min = as.Date(dat$time_range$from),
      max = as.Date(dat$time_range$to),
      value = as.Date(dat$date_default)
    )
  })
  
  output$optimaltime_time_input <- renderUI({
    # http://stackoverflow.com/questions/27198515/ui-elements-for-selecting-date-and-time-not-just-date-in-shiny
    # see: global.R
    inlineTimeInput(
      "optimaltime_time",
      "Time:",
      value = dat$time_default,
      seconds = FALSE
    )
  })
  
  output$optimaltime_earliest_input <- renderUI({
    # http://stackoverflow.com/questions/27198515/ui-elements-for-selecting-date-and-time-not-just-date-in-shiny
    # see: global.R
    # flog.info("dat$time_default_earliest = ", dat$time_default_earliest, capture = T)
    inlineTimeInput(
      "optimaltime_earliest_time",
      NULL,
      value = dat$time_default_earliest,
      seconds = FALSE
    )
  })
  
  output$optimaltime_latest_input <- renderUI({
    # http://stackoverflow.com/questions/27198515/ui-elements-for-selecting-date-and-time-not-just-date-in-shiny
    # see: global.R
    inlineTimeInput(
      "optimaltime_latest_time",
      NULL,
      value = dat$time_default_latest,
      seconds = FALSE
    )
  })
  
  #################################################
  # Output                                        #  
  #################################################
  
  ## draw the initial map
  output$map_optimaltime <- renderLeaflet({
    raster_pal_morgen <- colorNumeric(palette = viridis(100, option = "B"),
                                      domain = raster_data[["morgen"]]@data@values,
                                      na.color = "#00000000")
    raster_pal_abend <- colorNumeric(palette = viridis(100, option = "B"),
                                     domain = raster_data[["abend"]]@data@values,
                                     na.color = "#00000000")
    
    leaflet() %>%
      addOnClickListner(category = "shape",
                        layerId = "optimaltime-bbox",
                        inputId = "optimaltime_bbox_click") %>% 
      addOnClickListner(category = "shape",
                        layerId = "search-radius",
                        inputId = "optimaltime_search_radius_click") %>%
      setView(
        lng = dat$map_center$lng,
        lat = dat$map_center$lat,
        zoom = 14
      ) %>%
      addTiles(group = "OpenStreetMap",
               options = providerTileOptions(noWrap = TRUE)) %>%
      # Add the bounding box
      addRectangles(
        lng1 = dat$bbox$lng_min,
        lat1 = dat$bbox$lat_min,
        lng2 = dat$bbox$lng_max,
        lat2 = dat$bbox$lat_max,
        layerId = "optimaltime-bbox",
        group = "bbox",
        options = pathOptions(className = "optimaltime-bbox")
      ) %>%
      hideGroup("bbox") %>%
      # Add thermal scans
      addRasterImage(
        raster_data[["morgen"]],
        colors = raster_pal_morgen,
        layerId = "raster_morgen",
        group = "raster_morgen",
        project = FALSE,
        opacity = 0.6
      ) %>%
      addRasterImage(
        raster_data[["abend"]],
        colors = raster_pal_abend,
        layerId = "raster_abend",
        group = "raster_abend",
        project = FALSE,
        opacity = 0.6
      ) %>% hideGroup("raster_morgen") %>% hideGroup("raster_abend")
    
  })
  
  output$optimaltime_results_table <- shiny::renderDataTable({
    tab <- data.table()
    if (!is.null(dat$optimaltime_table)){
      tab <- dat$optimaltime_table 
      tab <- data.table(tab)
      tab <- tab[, !(names(tab) %in% c("location", "path")), with = F]
    } 
    if (nrow(tab) == 0)
      return(NULL)
    tab[, optimal_time := format(strptime(optimal_time, format = "%Y-%m-%dT%H:%M"), format = "%T")]
    tab[, duration := formatDurationMillis(duration, print.millis = F)]
    tab <-
      tab[, .(rank,
              name,
              optimal_time,
              distance,
              duration,
              opening_hours)]
    tab
  })
  
  
  #################################################
  # Handle Events                                 #  
  #################################################
  
  # observe input elements and update the ui; a new server request is required,
  # because those inputs effecting the query paramerter 
  observeEvent(
    c(
      input$navbar, # required to trigger a (re-)drawing of the map, when a different tab is selected
      input$optimaltime_start,
      input$optimaltime_place_type,
      input$optimaltime_date,
      input$optimaltime_time,
      input$optimaltime_radius,
      input$optimaltime_timebuffer,
      input$optimaltime_earliest_enabled,
      input$optimaltime_latest_enabled,
      input$optimaltime_rankby,
      input$optimaltime_max_results
    ),
    {
      handleOptimalTimeInput()
      renderOptimalTime()
    }
  )
  
  observeEvent(input$optimaltime_earliest_time, {
    if (input$optimaltime_earliest_enabled) {
      handleOptimalTimeInput()
      renderOptimalTime()
    }
  })
  
  observeEvent(input$optimaltime_latest_time, {
    if (input$optimaltime_latest_enabled) {
      handleOptimalTimeInput()
      renderOptimalTime()
    }
  })
  
  # show or hide the elements, no new server request required
  observeEvent(input$optimaltime_display_options, {
    map <- leafletProxy("map_optimaltime")
    
    # show or hide the search radius cirlce
    if ("show_radius" %in% input$optimaltime_display_options) {
      map %>% showGroup("radius")
    } else {
      map %>% hideGroup("radius")
    }
    
    # show or hide the bounding box
    if ("show_bbox" %in% input$optimaltime_display_options) {
      map %>% showGroup("bbox")
    } else {
      map %>% hideGroup("bbox")
    }
    
    # show or hide thermal scan
    if ("show_thermalscan" %in% input$optimaltime_display_options) {
      if (input$optimaltime_time < strptime("12:00:00", "%T")) {
        map %>% showGroup("raster_morgen")
      } else {
        map %>% showGroup("raster_abend")
      }
    } else {
      map %>% hideGroup("raster_morgen") %>% hideGroup("raster_abend")
    }
    
  }, ignoreNULL = FALSE)
  
  observeEvent(input$optimaltime_show_routes, {
    rank <- input$optimaltime_show_routes$rank
    dest_lat <- dat$optimaltime_json$results$location[[rank]][1]
    dest_lng <- dat$optimaltime_json$results$location[[rank]][2]
    opt_time <- strptime(dat$optimaltime_json$results$optimal_time[[rank]], format = "%Y-%m-%dT%H:%M")
    updateTextInput(session, "routing_start", value = input$optimaltime_start)
    updateTextInput(session, "routing_destination", value = paste0(dest_lat, ",", dest_lng))
    updateDateInput(session, "routing_date", value = opt_time)
    updateTimeInput(session, "routing_time", value = opt_time)
    updateNavbarPage(session, "navbar", selected = "Routing")
  })
  
  observeEvent(input$optimaltime_select_start, {
    if (dat$optimaltime_select_start_clicked == TRUE) {
      dat$optimaltime_select_start_clicked <- FALSE
    } else {
      dat$optimaltime_select_start_clicked <- TRUE
    }
    session$sendCustomMessage(
      type = 'optimaltime_select_start',
      message = list(clicked = dat$optimaltime_select_start_clicked)
    )
  })
  
  observeEvent(input$map_optimaltime_click, {
    click <- input$map_optimaltime_click
    clat <- click$lat
    clng <- click$lng
    if (dat$optimaltime_select_start_clicked) {
      updateTextInput(session, "optimaltime_start", value = paste0(clat, ",", clng))
    }
  })
  
  observeEvent(input$optimaltime_search_radius_click, {
    click <- input$optimaltime_search_radius_click
    clat <- click$lat
    clng <- click$lng
    if (dat$optimaltime_select_start_clicked) {
      updateTextInput(session, "optimaltime_start", value = paste0(clat, ",", clng))
    }
  })
  
  observeEvent(input$optimaltime_bbox_click, {
    click <- input$optimaltime_bbox_click
    clat <- click$lat
    clng <- click$lng
    if (dat$optimaltime_select_start_clicked) {
      updateTextInput(session, "optimaltime_start", value = paste0(clat, ",", clng))
    }
  })
  
  #################################################
  # Draw paths if a marker i                      #  
  #################################################
  
  # Event tiggered by the markerClickHandler function in message-handler.js
  observeEvent(input$map_optimaltime_marker_click, {
    flog.debug("clicked marker = ", input$map_optimaltime_marker_click, capture = T)
    
    id <- input$map_optimaltime_marker_click$id
    if (is.null(id))
      return(NULL)
    
    rank <- as.integer(gsub("^.*_(\\d+)$", "\\1", id))
    line_types <- c("optimal", "shortest")
    factpal <-
      colorFactor(rev(gg_color_hue(length(line_types
      ))), line_types)
    
    coords_opt <- as.data.frame(dat$optimaltime_json$results$path_optimal[[rank]])
    names(coords_opt) <- c("lat", "lng")
    coords_shortest <- as.data.frame(dat$optimaltime_json$results$path_shortest[[rank]])
    names(coords_shortest) <- c("lat", "lng")
    
    map <- leafletProxy("map_optimaltime")
    map %>% 
      removeShape("optimaltime_path_optimal") %>% 
      removeShape("optimaltime_path_shortest") %>% 
      removeControl("optimaltime_path_legend") %>%
      addPolylines(
        data = coords_opt,
        lat = ~ lat,
        lng = ~ lng,
        layerId = "optimaltime_path_optimal",
        color = factpal("optimal"),
        opacity = 0.75
      ) %>% 
      addPolylines(
        data = coords_shortest,
        lat = ~ lat,
        lng = ~ lng,
        layerId = "optimaltime_path_shortest",
        color = factpal("shortest"),
        opacity = 0.75
      ) %>% 
      addLegend(title = "Routes", pal = factpal,
                  values = line_types,
                  layerId = "optimaltime_path_legend")
  })
  
  
  #################################################
  # Server reques                                 #  
  #################################################
  
  handleOptimalTimeInput <- reactive({
    start_point <- checkLocation(
      input$optimaltime_start,
      name = "start",
      bbox = dat$bbox,
      shiny_session = session,
      anchorId = "optimaltime_alert_start",
      alertId = "optimaltime_alert_start_id"
    )
    
    if (is.null(start_point))
      return(NULL)
    
    # the time input is not valid
    if (!is.null(input$optimaltime_time) && is.na(input$optimaltime_time))
      return(NULL)
    
    dat$optimaltime_start_point <- start_point
    start_point
  })
  
  requestOptimaltime <- reactive({
    start_point <- dat$optimaltime_start_point 
    
    if (is.null(input$optimaltime_time) || is.na(input$optimaltime_time))
      return(NULL)

    if (is.Date(input$optimaltime_date)) {
      req_date <- input$optimaltime_date
    } else {
      req_date <- dat$date_default
    }
    
    if (is.POSIXlt(input$optimaltime_time)) {
      req_time <- input$optimaltime_time
    } else {
      req_time <- dat$time_default
    }
    
    req_date_time <- asRequestDateTime(req_date, req_time)
    
    if (req_date_time < dat$time_range$from ||
        req_date_time > dat$time_range$to) {
      createAlert(
        session = session,
        anchorId = "optimaltime_alert_datetime",
        alertId = "optimaltime_alert_datetime_id",
        content = paste0(
          "date time'", req_date_time,
          "' is not within the supported time range: ",
          dat$time_range$from ,
          " to ",
          dat$time_range$to
        ), 
        style = "danger",
        append = F
      )
      return(NULL)
    } else {
      closeAlert(session, "optimaltime_alert_datetime_id")
    }
    
    earliesttime <-
      if (input$optimaltime_earliest_enabled)
        asRequestDateTime(req_date, input$optimaltime_earliest_time)
      else
        NULL
    latesttime <-
      if (input$optimaltime_latest_enabled)
        asRequestDateTime(req_date, input$optimaltime_latest_time)
    else
      NULL
    
    if (input$optimaltime_earliest_enabled 
        && (is.null(input$optimaltime_earliest_time) 
            || is.na(input$optimaltime_earliest_time))) 
      return(NULL)
    
    if (input$optimaltime_latest_enabled 
        && (is.null(input$optimaltime_latest_time) 
            || is.na(input$optimaltime_latest_time))) 
      return(NULL)
    
    if (input$optimaltime_earliest_enabled 
        && input$optimaltime_latest_enabled 
        && input$optimaltime_earliest_time >= input$optimaltime_latest_time) {
      createAlert(session = session, 
                  anchorId = "optimaltime_alert_latest",
                  alertId = "optimaltime_alert_latest_id",
                  title = "Oops!", 
                  content = "earliest time must be before latest time",
                  style = "danger",
                  append = F)
      return(NULL)
    } else {
      # close any previous created alert
      closeAlert(session, "optimaltime_alert_latest_id") 
    }
      
    res <- getOptimalTime(
      baseurl = BACKEND_OPTIMALTIME_URL,
      start = start_point,
      time = req_date_time,
      place_type = input$optimaltime_place_type,
      max_distance = input$optimaltime_radius,
      max_results = input$optimaltime_max_results,
      time_buffer = input$optimaltime_timebuffer,
      earliest_time = earliesttime,
      latest_time = latesttime
    )
    
    if (res$status == "NO_REULTS") {
      dat$optimaltime_table <- NULL
      createAlert(
        session = session,
        anchorId = "optimaltime_alerts",
        alertId = "optimaltime_alerts_id",
        title = "No Results Found!",
        style = "info",
        append = F
      )
      return(NULL)
    }
  
    if (res$status != "OK") {
      createAlert(session = session, 
                  anchorId = "optimaltime_alerts",
                  alertId = "optimaltime_alerts_id",
                  title = "Oops! An Internal Server Error Occourd", 
                  content = res$json$messages,
                  style = "danger",
                  append = F)
      return(NULL)
    }
    
    closeAlert(session, "optimaltime_alerts_id")
    
    dat$optimaltime_json <- res$json
    
    res
  })

  # computeOptimalTimeTable <- reactive({
  #   res <- NULL
  #   if (dat$optimaltime_json)
  #   dat$optimaltime_table <- res
  #   res
  # })
  
  #################################################
  # Update map                                    #  
  #################################################
  
  renderOptimalTime <- reactive({
    
    flog.debug("renderOptimalTime")
    
    withProgress(message = "Requesting optimal time...", {
      res <- requestOptimaltime()
      incProgress(0.5, message = "Drawing map...")
      
      
      
      # res_json <- dat$optimaltime_json
      res_json <- res$json
      
      map <- leafletProxy("map_optimaltime")
      
      start_point <- dat$optimaltime_start_point
      map %>% clearMarkers() %>% clearShapes()

      # re-add the bounding box
      map %>% addRectangles(
        lng1 = dat$bbox$lng_min,
        lat1 = dat$bbox$lat_min,
        lng2 = dat$bbox$lng_max,
        lat2 = dat$bbox$lat_max,
        layerId = "optimaltime-bbox",
        group = "bbox",
        options = pathOptions(className = "optimaltime-bbox")
      )
      
      if ("show_bbox" %in% input$optimaltime_display_options) {
        map %>% showGroup("bbox")
      } else {
        map %>% hideGroup("bbox")
      }
      
      # show or hide thermal scan
      if ("show_thermalscan" %in% input$optimaltime_display_options) {
        if (input$optimaltime_time < strptime("12:00:00", "%T")) {
          map %>% showGroup("raster_morgen")
        } else {
          map %>% showGroup("raster_abend")
        }
      } else {
        map %>% hideGroup("raster_morgen") %>% hideGroup("raster_abend")
      }
      
      if (!is.null(start_point)) {
        map %>% addMarkers(lng = start_point$lng, lat = start_point$lat)
        if ("show_radius" %in% input$optimaltime_display_options) {
          map %>% addCircles(
            lng = start_point$lng,
            lat = start_point$lat,
            radius = input$optimaltime_radius,
            layerId = "search-radius",
            group = "radius",
            options = pathOptions(className = "search-radius")
          )
        }
      }
      if (!is.null(res_json) && res_json$status == "OK") {
        res_dt <- data.table(data.frame(res_json$results))
        res_dt[, rank_value := rank]
        res_dt[, rank_distance := frank(res_dt, distance)]
        
        if (input$optimaltime_rankby == "distance") {
          res_dt[, rank := rank_distance]
        }
        
        res_dt <- res_dt[order(rank), ]
        dat$optimaltime_table <- res_dt
        marker_pal <-
          colorNumeric(palette = rev(gg_color_hue(length(unique(res_dt$rank)))),
                       domain = order(unique(res_dt$rank)),
                       na.color = "#00000000")
        cols <-
          c("rank",
            "name",
            "optimal_time",
            "optimal_value",
            "distance",
            "duration",
            "opening_hours")
        # Add the markers in reverse order, so that 
        # the label with the best rank is displayed on top.
        for (r in rev(unique(res_dt$rank))) {
          lat <- res_dt[rank == r,]$location[[1]][[1]]
          lng <- res_dt[rank == r,]$location[[1]][[2]]
          marker_popup <- popupTableOptimalTime(res_dt[rank == r,], cols)
          marker_popup <- tags$div(
            marker_popup,
            tags$a(
              id = paste0("optimaltime_show_routes_", r),
              class = "action-button",
              class = "show-routes",
              "Show routes"
            )
          )
          map %>% addCircleMarkers(
            lng = lng,
            lat = lat,
            radius = 5,
            color = marker_pal(r),
            opacity = 1,
            fillColor = marker_pal(r),
            fillOpacity = 1,
            layerId = paste0("optimaltime_marker_", r),
            popup = as.character(marker_popup),
            label = as.character(r),
            labelOptions = labelOptions(
              noHide = TRUE,
              direction = "auto"
            )
          )
        }
      }
    })
  })
  
})
