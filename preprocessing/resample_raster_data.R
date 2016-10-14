#!/usr/bin/env RScript --vanilla

if (!require("optparse"))
  install.packages("optparse")
if (!require("tools"))
  install.packages("tools")
if (!require("rgdal"))
  install.packages("rgdal")
if(!require("raster"))
  install.packages("raster")

args <- commandArgs(trailingOnly=TRUE)

option_list = list(
  make_option(
    c("--input"),
    action = "store",
    type = "character",
    help = "The raster file (GeoTiff) to scale; multiple raster files can be separated by a colon (':')"
  ),
  make_option(
    c("--output"),
    action = "store",
    type = "character",
    default = NULL,
    help = "The scaled raster file [default INPUT_PIXELpx.tif]"
  ),
  make_option(
    c("--pixel"),
    action = "store",
    type = "integer",
    default = 1000000,
    help = "The maximum number of pixel [default %default]"
  )
)

opt <- parse_args(OptionParser(option_list = option_list))

in_files <- unlist(strsplit(opt$input, ":"))

if (is.null(opt$output)) {
  out_files <- character()
  for (file in in_files) {
    file_name <- tools::file_path_sans_ext(file)
    flle_extension <- tools::file_ext(file)
    out_file <- paste0(file_name, "_", opt$pixel, "px.", flle_extension)
    out_files <- cbind(out_files, out_file)
  }
} else {
  out_files <- unlist(strsplit(opt$output, ":"))
  if (length(in_files) != length(out_files)) 
    stop("if '--output' is provided it must have the same number of file names as '--input'")
}

for (i in seq_along(in_files)) {
  raster_data <- raster::raster(rgdal::readGDAL(in_files[[i]]))
  raster_data <- raster::sampleRegular(raster_data, opt$pixel, asRaster = T)
  raster::writeRaster(x = raster_data, filename = out_files[[i]], overwrite = T)
}
