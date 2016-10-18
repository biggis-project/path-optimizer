if (!require("pacman"))
  install.packages("pacman")

# load and install the required packages if necessary
pacman::p_load(
  "futile.logger",
  "optparse",
  "rgdal",
  "rgeos",
  "raster",
  "osmar",
  "dismo",
  "geosphere",
  "data.table",
  "profvis"
)

############################################################################
#
# Functions
#
############################################################################

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

getWayNodes <- function(osmar_obj, way_id) {
  stopifnot("osmar" %in% class(osmar_obj))
  stopifnot(length(way_id) == 1)
  way_node_ids <- find_down(osmar_obj, way(way_id))$node_ids
  way_nodes <- subset(osmar_obj, node_ids = way_node_ids)
  ret <- way_nodes$nodes$attrs[, c("id", "lon", "lat")]
  ret$way_id <- way_id
  return(ret)
}

mergeWeightedLinesWithNodeIds <-  function(lines_df, osmar_obj) {
  stopifnot(is.data.frame(lines_df))
  stopifnot("osmar" %in% class(osmar_obj))
  
  lines_df <- data.table(lines_df)
  
  lines_df[, from.osm.id := NA_real_]
  lines_df[, to.osm.id := NA_real_]
  
  # TODO: find a more elegant way
  way_nodes <- data.table()
  for (wayid in unique(lines_df$way_id)) {
    way_nodes <- rbind(way_nodes, getWayNodes(osmar_obj, wayid))
  }
  
  # TODO: find a more elegant way
  for (i in 1:nrow(lines_df)) {
    from <- lines_df[i, .(way_id, from.lon, from.lat)]
    to <- lines_df[i, .(way_id, to.lon, to.lat)]
    from_id <-
      unique(way_nodes[way_id == from$way_id &
                         lon == from$from.lon &
                         lat == from$from.lat]$id)
    to_id <- unique(way_nodes[way_id == to$way_id &
                                lon == to$to.lon &
                                lat == to$to.lat]$id)
    stopifnot(length(from_id) == 1)
    stopifnot(length(to_id) == 1)
    lines_df[i, from.osm.id := from_id]
    lines_df[i, to.osm.id := to_id]
  }
  return(lines_df)
}

# Split the lines in segements
getLineSegments <- function(sp_lines) {
  stopifnot(class(sp_lines) == "SpatialLinesDataFrame")
  
  res_lines <- list()
  res_data <- data.frame()
  res_crs <- crs(sp_lines)
  
  for (l in sp_lines@lines) {
    # required?
    for (i in 1:length(l@Lines)) {
      line_points <- as.matrix(l@Lines[[1]]@coords)
      line_way_id <- l@ID
      prev <- NULL
      for (j in 1:nrow(line_points)) {
        line_point <- line_points[j, ]
        line_id <- rownames(line_points)[j]
        if (!is.null(prev)) {
          new_line <- rbind(prev$coords, line_point)
          rownames(new_line) <- c(prev$line_id, line_id)
          new_line_id <-
            paste(line_way_id, prev$line_id, line_id, sep = "-")
          new_line <-
            Lines(Line(coords = new_line), ID = new_line_id)
          res_lines <- c(res_lines, new_line)
          res_tmp <- data.frame(list(
            way_id = as.integer(line_way_id),
            from = as.integer(prev$line_id),
            to = as.integer(line_id)
          ))
          rownames(res_tmp) <- c(new_line_id)
          res_data <-
            rbind(res_data, res_tmp)
        }
        prev <-
          list(coords = line_point,
               line_id = line_id,
               line_way_id = line_way_id)
      }
    }
  }
  stopifnot(length(res_lines) == nrow(res_data))
  res_sp_lines <- SpatialLines(res_lines, res_crs)
  res <- SpatialLinesDataFrame(res_sp_lines, res_data)
  return(res)
}

getWeightedSegements <- function(line_segments,
                                 raster_obj,
                                 method = c("all", "weightedsum", "mean")) {
  method <- match.arg(method)
  stopifnot(class(line_segments) == "SpatialLinesDataFrame")
  stopifnot(class(raster_obj) == "RasterLayer")
  
  ret <- data.table()
  
  flog.info(
    "getWeightedSegements: %s line segments, %s raster cells, method = %s",
    length(line_segments),
    length(raster_obj),
    method
  )
  
  flog.debug("getWeightedSegements: extent(raster_obj) = ",
             extent(raster_obj),
             capture = T)
  
  # rasterIntersect <- tryCatchFactory(raster::intersect)
  tryCrop <- tryCatchFactory(raster::crop)
  
  for (i in 1:length(line_segments)) {
    if ((i - 1) %% 1000 == 0)
      flog.debug("getWeightedSegements: line segment %s", i)
    line_segment <- line_segments[i,]
    # line_intersec <- rasterIntersect(raster_obj, line_segment)
    
    eps <- 0.000001
    
    line_segment_extent <- extent(line_segment)
    # if line_segment is a vertical or horiontal line (i.e. xmin == xmax or ymin == ymax)
    # we have to ensure, that the line_segment_extent is not empty
    if (line_segment_extent@xmin == line_segment_extent@xmax) {
      line_segment_extent@xmin <- line_segment_extent@xmin - eps
      line_segment_extent@xmax <- line_segment_extent@xmax + eps
    }
    if (line_segment_extent@ymin == line_segment_extent@ymax) {
      line_segment_extent@ymin <- line_segment_extent@ymin - eps
      line_segment_extent@ymax <- line_segment_extent@ymax + eps
    }
    
    line_intersec <-
      tryCrop(raster_obj, line_segment_extent, snap = "out")
    
    
    # TODO: how to deal properly with missing data?
    if (!is.null(line_intersec$err)) {
      flog.debug("error in line_intersect: %s", line_intersec$err)
      flog.debug("extent line_segment = ", extent(line_segment), capture = T)
      next
    } else {
      if (!is.null(line_intersec$warn))
        flog.warn("warning(s) in rasterIntersect",
                  line_intersec$warn,
                  capture = T)
      line_intersec <- line_intersec$res
    }
    
    if (is.null(line_segment@lines)) {
      flog.debug("line_segment@lines", line_segments@lines, capture = T)
    }
    
    res <- list(
      way_id = line_segment@data$way_id,
      from = line_segment@data$from,
      to = line_segment@data$to,
      from.lon = line_segment@lines[[1]]@Lines[[1]]@coords[1, 1],
      from.lat = line_segment@lines[[1]]@Lines[[1]]@coords[1, 2],
      to.lon = line_segment@lines[[1]]@Lines[[1]]@coords[2, 1],
      to.lat = line_segment@lines[[1]]@Lines[[1]]@coords[2, 2],
      dist = LineLength(line_segment@lines[[1]]@Lines[[1]], longlat = T) * 1000
    )
    
    if (method == "all") {
      res$delta_temp <- NA_real_
    } else if (method == "weightedsum") {
      res$weight <- NA_real_
      res$mean_temp <- NA_real_
    }
    
    if (method == "all" || method == "weightedsum") {
      # weighted sum
      # idea: find for every intersected raster cell the inetsection with the line
      # and compute the length
      raster_overlay <- rasterToPolygons(line_intersec)
      crs(raster_overlay) <- crs(line_segment)
      tmp <- data.table()
      for (j in 1:length(raster_overlay)) {
        intersec <- gIntersection(raster_overlay[j, ], line_segment)
        
        if (is.null(intersec)) {
          # flog.trace("intersec is null")
          next
        }
        if ("SpatialPoints" %in% class(intersec)) {
          # intersection is only a point, so we set dist to 0
          tmp <-
            rbind(tmp, list(dist = 0,
                            value = raster_overlay@data[j, ]))
        } else {
          # intesection is a line
          dist <-
            LineLength(intersec@lines[[1]]@Lines[[1]], longlat = T) * 1000 # in meter
          tmp <-
            rbind(tmp, list(dist = dist,
                            value = raster_overlay@data[j, ]))
        }
      }
      if (nrow(tmp) == 0) {
        flog.debug("tmp is empty")
        next
      }
      if (method == "all") {
        for (k in 1:nrow(tmp)) {
          res$dist <- tmp[k,]$dist
          res$delta_temp <- tmp[k,]$value
          
          ret <- rbind(ret, res)
        }
      } else if (method == "weightedsum") {
        res$dist <- sum(tmp[, dist])
        res$mean_temp <- mean(tmp[, value])
        res$weight <- sum(tmp[, dist] * tmp[, value])
        
        ret <- rbind(ret, res)
      }
    } else {
      stop(paste("method", method, "not yet implemented"))
    }
  }
  return(ret)
}

extractHighWays <- function(osmar_obj) {
  stopifnot("osmar" %in% class(osmar_obj))
  hw_ids <-
    find(osmar_obj, way(tags(
      k == "highway" |
        (k == "railway" & v == "platform") |
        (k == "public_transport" & v == "platform")
      # | (k == "sidewalk" & v %in% c("yes", "both", "left", "right"))
    )))
  hw_ids <- find_down(osmar_obj, way(hw_ids))
  hw <- subset(osmar_obj, ids = hw_ids)
  return(hw)
}



computeWeightedEdges <- function(raster_obj, osm_hw, osm_hw_sp, plot = F) {
  if (plot == TRUE) {
    plot(raster_obj, col = rev(heat.colors(255)))
    plot(osm_hw_sp$lines, add = T)
  }
  
  # compute devation from the mean
  raster_obj_mean <- mean(raster_obj@data@values, na.rm = T)
  flog.info("compute deviations from the mean value (mean = %s)",
            raster_obj_mean)
  raster_obj@data@values <- raster_obj@data@values - raster_obj_mean
  
  flog.info("compute line segments")
  t1 <- Sys.time()
  system.time({
    line_segments <- getLineSegments(osm_hw_sp$lines)
    # crs(raster_obj) <- crs(line_segments)
  })
  flog.info(
    "computed %s line segments in %s seconds",
    length(line_segments),
    difftime(Sys.time(), t1, units = "secs")
  )
  
  flog.info("compute weighted line segments")
  t1 <- Sys.time()
  system.time(weighted_line_segments <-
                getWeightedSegements(line_segments, raster_obj))
  flog.info(
    "computed %s weighted line segments in %s mins",
    nrow(weighted_line_segments),
    difftime(Sys.time(), t1, units = "mins")
  )
  
  weighted_line_segments_dt <- weighted_line_segments
  
  flog.info("join weighted lines with osm node ids")
  t1 <- Sys.time()
  system.time(
    weighted_lines <-
      mergeWeightedLinesWithNodeIds(weighted_line_segments_dt,
                                    osm_hw)
  )
  flog.info(
    "joined %s rows in %s mins",
    nrow(weighted_lines),
    difftime(Sys.time(), t1, units = "mins")
  )
  
  return(weighted_lines)
}


############################################################################
#
# Workflow
#
############################################################################
weightedLines <-
  function(osm_file,
           raster_morgen_georect_file,
           raster_abend_georect_file,
           out_file,
           bounding_box = c(
             left = 8.38500,
             bottom = 48.99000,
             right = 8.43500,
             top = 49.02500
           ),
           osmar_file = NULL,
           print.plots = FALSE) {
    
    bbox <-
      osmar::corner_bbox(bounding_box[["left"]],
                         bounding_box[["bottom"]],
                         bounding_box[["right"]],
                         bounding_box[["top"]])
    
    if (!is.null(osmar_file) && file.exists(osmar_file)) {
      flog.info("loading osmar data set from %s", osmar_file)
      load(file = osmar_file, verbose = T)
    } else {
      flog.info("reading osm file %s", osm_file)
      system.time(osm <-
                    osmar::get_osm(x = bbox, source =  osmsource_file(osm_file)))
      flog.info("done")
      if (!is.null(osmar_file)) {
        save("osm", file = osmar_file)
      }
    }
    
    flog.info("extracting high ways")
    osm_hw <- extractHighWays(osm)
    
    flog.info("converting highways to spatial lines")
    system.time(osm_hw_sp <- as_sp(osm_hw))
    
    flog.info("splite ways in line segments")
    system.time(line_segments <- getLineSegments(osm_hw_sp$lines))
    
    osm_hw_sp_extent <- extent(osm_hw_sp$lines)
    raster_extent <- extent(osm_hw_sp$lines)
    
    flog.info("load raster data")
    raster_morgen_georect <-
      raster::raster(readGDAL(raster_morgen_georect_file))
    raster_morgen_georect <-
      crop(raster_morgen_georect, extent(osm_hw_sp$lines))
    
    raster_abend_georect <-
      raster::raster(readGDAL(raster_abend_georect_file))
    raster_abend_georect <-
      crop(raster_abend_georect, extent(osm_hw_sp$lines))
    
    if (print.plots == TRUE) {
      plot(raster_morgen_georect, col = rev(heat.colors(255)))
      plot(osm_hw_sp$lines, add = T)
      
      plot(raster_abend_georect, col = rev(heat.colors(255)))
      plot(osm_hw_sp$lines, add = T)
    }
    
    flog.info("compute weighted lines 'morgen'")
    weighted_lines_morgen <-
      computeWeightedEdges(raster_morgen_georect, osm_hw, osm_hw_sp)
    
    flog.info("compute weighted lines 'abend'")
    weighted_lines_abend <-
      computeWeightedEdges(raster_abend_georect, osm_hw, osm_hw_sp)
    
    weighted_lines_morgen[, time_range := "morgen"]
    
    weighted_lines_abend[, time_range := "abend"]
    
    weighted_lines_combined <-
      rbind(weighted_lines_morgen, weighted_lines_abend)
    
    flog.info("write results to %s", out_file)
    write.table(
      weighted_lines_combined[, .(
        way_id,
        from.osm.id,
        to.osm.id,
        from.lon,
        from.lat,
        to.lon,
        to.lat,
        dist,
        delta_temp,
        time_range
      )],
      out_file,
      quote = F,
      sep = "|",
      row.names = F
    )
  }

# flog.threshold(DEBUG)
# weightedLines(
#   osm_file = OSM_FILE,
#   raster_morgen_georect_file = RASTER_MORGEN_GEORECT,
#   raster_abend_georect_file = RASTER_ABEND_GEORECT,
#   out_file = OUT_FILE_COMBINED
# )
