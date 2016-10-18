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
# Script
#
############################################################################

invisible(flog.threshold(INFO))

args <- commandArgs(trailingOnly = TRUE)

# detect the dir were the scripts are located
# (which can differ from the current working dir)
# See: http://stackoverflow.com/a/1815743
initial.options <- commandArgs(trailingOnly = FALSE)
file.arg.name <- "--file="
script.name <-
  sub(file.arg.name, "", initial.options[grep(file.arg.name, initial.options)])
script.basename <- dirname(script.name)

option_list = list(
  make_option(
    c("--osm_file"),
    action = "store",
    type = 'character',
    help = "The OpenStreetMap data set in the OSM XML format."
  ),
  make_option(
    c("--raster_morning"),
    action = "store",
    type = 'character',
    help = "The thermal scan 'morning' as GeoTiff file."
  ),
  make_option(
    c("--raster_evening"),
    action = "store",
    type = 'character',
    help = "The thermal scan 'evening' as GeoTiff file."
  ),
  make_option(
    c("--output"),
    action = "store",
    type = 'character',
    help = "The output file (will be replaced)."
  ),
  make_option(
    c("--bbox_left"),
    action = "store",
    type = 'double',
    help = "The minimum longitude value of the bounding box."
  ),
  make_option(
    c("--bbox_bottom"),
    action = "store",
    type = 'double',
    help = "The minimum latitude value of the bounding box."
  ),
  make_option(
    c("--bbox_right"),
    action = "store",
    type = 'double',
    help = "The maximum longitude value of the bounding box."
  ),
  make_option(
    c("--bbox_top"),
    action = "store",
    type = 'double',
    help = "The maximum latitude value of the bounding box."
  ),
  make_option(
    c("-v", "--verbose"),
    action = "store_true",
    default = FALSE,
    help = "Should debug messages be printed? [default %default]"
  )
)

opt <- parse_args(OptionParser(option_list = option_list))

if (opt$v) {
  invisible(flog.threshold(DEBUG))
}

required_params <- c(
  "osm_file",
  "raster_morning",
  "raster_evening",
  "output",
  "bbox_left",
  "bbox_bottom",
  "bbox_right",
  "bbox_top"
)

for (param in required_params) {
  if (is.null(opt[[param]]))
    stop(paste0("Parameter '", param, "' is missing. See script usage (--help)"))
}

flog.debug("working dir = %s", getwd())

flog.info("executing weightedLines()")
flog.debug(
  paste(
    "weightedLines(osm_file = %s,",
    "raster_morgen_georect_file = %s,",
    "raster_abend_georect_file = %s,",
    "out_file = %s, ...)"
  ),
  normalizePath(opt$osm_file),
  normalizePath(opt$raster_morning),
  normalizePath(opt$raster_evening),
  normalizePath(opt$output)
)
flog.debug(
  "bounding box: left = %s, bottom = %s, right = %s, top = %s",
  opt$bbox_left,
  opt$bbox_bottom,
  opt$bbox_right,
  opt$bbox_top
)

# load the actual implementation
source(paste(script.basename, "weightedlines_impl.R", sep = "/"))

weightedLines(
  osm_file = opt$osm_file,
  raster_morgen_georect_file = opt$raster_morning,
  raster_abend_georect_file = opt$raster_evening,
  out_file = opt$output,
  bounding_box = c(
    left = opt$bbox_left,
    bottom = opt$bbox_bottom,
    right = opt$bbox_right,
    top = opt$bbox_top
  )
)
