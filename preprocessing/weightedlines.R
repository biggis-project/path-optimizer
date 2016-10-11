#!/usr/bin/env RScript --vanilla


############################################################################
#
# Script to created weighted way segments based on an OSM file and 
# thermal scan data. 
#
# Run 'RScript --vanilla weightedlines.R --help' to print a help message. 
#
############################################################################


if (!require("futile.logger"))
  install.packages("futile.logger")
if (!require("optparse"))
  install.packages("optparse")

############################################################################
#
# Config
#
############################################################################

OSM_FILE <- normalizePath("../src/main/resources/data/karlsruhe.osm")

# georeferenced data of the thermal scan
RASTER_MORGEN_GEORECT <- normalizePath("../shiny-frontend/HeatStressRouting-Frontend/data/raster_morgen_georect.tif")
RASTER_ABEND_GEORECT <- normalizePath("../shiny-frontend/HeatStressRouting-Frontend/data/raster_abend_georect.tif")

OUT_DIR <- normalizePath("../src/main/resources/data/")
OUT_FILE_COMBINED <- paste0(OUT_DIR, "/weighted_lines.csv")

# OSMAR_FILE <- normalizePath("./osmar_karlsruhe.RData")

############################################################################
#
# Script
#
############################################################################

# executed as script
if (!interactive()) {
  flog.threshold(INFO)
  
  args <- commandArgs(trailingOnly=TRUE)
  flog.debug("args = ", args, capture = T)
  
  option_list = list(
    make_option(c("--osm_file"), action="store", default=OSM_FILE, type='character',
                help="the OpenStreetMap data set in the OSM XML format"),
    make_option(c("--raster_morgen"), action="store", default=RASTER_MORGEN_GEORECT, type='character',
                help="the thermal scan 'morgen' as GeoTiff file"),
    make_option(c("--raster_abend"), action="store", default=RASTER_ABEND_GEORECT, type='character',
                help="the thermal scan 'abend' as GeoTiff file"),
    make_option(c("--output"), action="store", default=OUT_FILE_COMBINED, type='character',
                help="the output file (will be replaced)"),
    make_option(c("-v", "--verbose"), action = "store_true", default = FALSE,
                help = "should debug messages be printed? [default %default]")
  )
  
  opt <- parse_args(OptionParser(option_list = option_list))
  
  if (opt$v) {
    flog.threshold(DEBUG)
  }
  
  flog.debug("working dir = %s", getwd())
  
  flog.info("info executing weightedLines()")
  flog.debug("weightedLines(osm_file = %s, raster_morgen_georect_file = %s, raster_abend_georect_file = %s), out_file = %s",
             normalizePath(opt$osm_file), normalizePath(opt$raster_morgen), normalizePath(opt$raster_abend), normalizePath(opt$output))
  
  # load the actual implementation
  source("weightedlines_impl.R")
  
  weightedLines(
    osm_file = normalizePath(opt$osm_file),
    raster_morgen_georect_file = normalizePath(opt$raster_morgen),
    raster_abend_georect_file = normalizePath(opt$raster_abend),
    out_file = normalizePath(opt$output)
  )
  
}