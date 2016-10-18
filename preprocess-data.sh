#!/bin/sh

###########################################################
# Config:
###########################################################
DATA_DIR="./data"
RESOURCES_DIR="./src/main/resources/data"
SHINY_DATA_DIR="./shiny-frontend/HeatStressRouting-Frontend/data"

OSM_URL="http://download.geofabrik.de/europe/germany/baden-wuerttemberg/karlsruhe-regbez-latest.osm.pbf"
OSM_FILE="$DATA_DIR/karlsruhe-regbez-latest.osm.pbf"
OSM_FILE_CROPPED="$DATA_DIR/karlsruhe.osm"

WEATHER_DATA_URL="ftp://ftp-cdc.dwd.de/pub/CDC/observations_germany/climate/hourly/air_temperature/recent/stundenwerte_TU_04177_akt.zip"
WEATHER_DATA_ZIP="$DATA_DIR/stundenwerte_TU_04177_akt.zip"
WEATHER_DATA_FILE="$DATA_DIR/weather_data.csv"

THERMAL_FLIGHT_MORNING="$DATA_DIR/thermal-flight-karlsruhe-morning.tif"
THERMAL_FLIGHT_EVENING="$DATA_DIR/thermal-flight-karlsruhe-evening.tif"
THERMAL_FLIGHT_PREFIX="$DATA_DIR/thermal-flight"
THERMAL_FLIGHT_PIXEL=1000000

WEIGHTED_LINES_FILE="$DATA_DIR/weighted_lines.csv"

BBOX_TOP="49.025"
BBOX_BOTTOM="48.99"
BBOX_LEFT="8.385"
BBOX_RIGHT="8.435"

###########################################################
# Arguments:
###########################################################
UPDATE=false
DEBUG=false
RECOMPUTE_WEIGHTS=false
SKIP_UPDATE_WEATER_DATA=false

###########################################################
# Variables:
###########################################################
UPDATED_OSM=false
UPDATED_THERMAL_FLIGHT=false


usage() {
	cat <<EOM
Usage: $0 [options]

  script that downloads the required data and performs some preprocessing

Options:  
	-h, --help             Show this usage message.
	--update               Download the newest files from the server
	                       even if they allready exists in '$DATA_DIR'.
	--recompute-weights    Forces a recomputation of the weighted lines
	                       Note: the recomputation can take very long.
	--debug                Enable debug output.
EOM
}

parse_args() {
	for arg in "$@"; do
		case $arg in
    		-h|--help)
    			usage
    			exit 0
    			;;
    		--update)
    			UPDATE=true
    			;;
    		--recompute-weights)
    			RECOMPUTE_WEIGHTS=true
    			;;
    		--debug)
    			DEBUG=true
    			;;
    		--skip-download-weather-data)  # TODO remove
    			SKIP_UPDATE_WEATER_DATA=true
    			;;
    		*)
    			# unknown option
    			;;
    	esac
	done
}

info() {
	echo "INFO:" "$@"
}

error() {
	echo "ERROR:" "$@"
}

debug() {
	if [ $DEBUG = true ]; then
		echo "DEBUG:" "$@"
	fi
}

download_osm_file() {	
	if [ ! -e $OSM_FILE ] || [ $UPDATE = true ]; then
	
		if [ -e $OSM_FILE ] && [ $UPDATE = true ]; then
			info "Updating file $OSM_FILE"
			info "Downloading newest version from $OSM_URL..."
		else 
			info "File $OSM_FILE not found"
			info "Downloading the missing file from $OSM_URL..."
		fi
		wget -q --show-progress -O $OSM_FILE $OSM_URL
		if [ ! $? ]; then 
			error "Failed to download $OSM_URL" 1>&2
			rm $OSM_FILE # remove the file, so it is downloaded the next time
			exit 1
		fi
		UPDATED_OSM=true
	else 
		info "File" $OSM_FILE "allready exists, so I'm going to use that" 
	fi

	if [ ! -e $OSM_FILE_CROPPED ] || [ $UPDATED_OSM = true ]; then
		
		# Check if Osmosis is installed
		if ! command -v "osmosis" > /dev/null 2>&1; then
	  		error "Please install Osmosis (https://wiki.openstreetmap.org/wiki/Osmosis) and add to the PATH" 1>&2
	  		exit 1
		fi
	
		info "Extracting bounding box (top=$BBOX_TOP left=$BBOX_LEFT bottom=$BBOX_BOTTOM right=$BBOX_RIGHT) from $OSM_FILE..."
		osmosis \
	 		--read-pbf $OSM_FILE \
			--bounding-box top=$BBOX_TOP left=$BBOX_LEFT bottom=$BBOX_BOTTOM right=$BBOX_RIGHT completeWays=yes \
			--write-xml $OSM_FILE_CROPPED
		
		if [ ! $? ]; then 
				error "Failed to extract bounding box from $OSM_FILE" 1>&2
				rm $OSM_FILE
				rm $OSM_FILE_CROPPED
				exit 1
		fi
	fi

	# Should mvn copy the file instead?
	info "Copy $OSM_FILE_CROPPED to $RESOURCES_DIR"
	cp $OSM_FILE_CROPPED $RESOURCES_DIR	
}

download_weather_data() {
	if [ ! -e $WEATHER_DATA_FILE ] || [ $SKIP_UPDATE_WEATER_DATA != true ]; then
		
		if [ ! -e $WEATHER_DATA_ZIP ] || [ $UPDATE = true ]; then
			info "Downloading the newest weather data from $WEATHER_DATA_URL..."
			wget -q --show-progress -O $WEATHER_DATA_ZIP $WEATHER_DATA_URL
			if [ ! $? ]; then 
				error "Failed to download $WEATHER_DATA_URL" 1>&2
				rm $WEATHER_DATA_ZIP # rm the file, so it is downloaded the next time
			exit 1
			fi
		fi
		
		WEATHER_DATA_FILE_NAME=$(unzip -Z1 $WEATHER_DATA_ZIP | grep -i "produkt_temp_Terminwerte")
		unzip -o $WEATHER_DATA_ZIP "$WEATHER_DATA_FILE_NAME" -d "$DATA_DIR/"
		if [ ! $? ]; then 
			error "Failed to unzip weather data $WEATHER_DATA_FILE_NAME from $WEATHER_DATA_ZIP" 1>&2
			rm $WEATHER_DATA_ZIP # rm the file, so it is downloaded the next time
			exit 1
		fi
		
		rm $WEATHER_DATA_ZIP # clean up zip file
		mv "$DATA_DIR/$WEATHER_DATA_FILE_NAME" $WEATHER_DATA_FILE # rename the downloaded weather data file
		
		info "Copy $WEATHER_DATA_FILE to $RESOURCES_DIR"
		cp $WEATHER_DATA_FILE $RESOURCES_DIR
	
	else
		info "Skipping update of file $WEATHER_DATA_FILE"
	fi
}

download_thermal_flight_files() {
	# TODO download raster data from the server	
	if [ ! -e $THERMAL_FLIGHT_MORNING ]; then
		error "File $THERMAL_FLIGHT_MORNING not found; please copy the file to $DATA_DIR"
		exit 1 
	fi

	if [ ! -e $THERMAL_FLIGHT_EVENING ]; then
		error "File $THERMAL_FLIGHT_EVENING not found; please copy the file to $DATA_DIR"
		exit 1
	fi

	# TODO set UPDATED_THERMAL_FLIGHT=true

	info "Resample $THERMAL_FLIGHT_MORNING and $THERMAL_FLIGHT_EVENING to $THERMAL_FLIGHT_PIXEL pixel..."
	
	Rscript --vanilla ./preprocessing/resample_raster_data.R \
		--input=$THERMAL_FLIGHT_MORNING":"$THERMAL_FLIGHT_EVENING \
		--pixel=$THERMAL_FLIGHT_PIXEL
	
	if [ ! $? ]; then 
		error "Failed to resample raster data $THERMAL_FLIGHT_MORNING and $THERMAL_FLIGHT_EVENING" 1>&2
		exit 1
	fi

	THERMAL_FLIGHT_DATA=$(find $THERMAL_FLIGHT_PREFIX*)
	info "Copy $(echo $THERMAL_FLIGHT_DATA | tr '\n' ' ') to $SHINY_DATA_DIR..."
	for file in $THERMAL_FLIGHT_DATA; do
		debug "Copy $file to $SHINY_DATA_DIR..."
		cp $file $SHINY_DATA_DIR
	done
}

compute_weighted_lines() {	
	debug "UPDATED_OSM = $UPDATED_OSM"
	debug "UPDATED_THERMAL_FLIGHT = $UPDATED_THERMAL_FLIGHT"
	
	if [ ! -e $WEIGHTED_LINES_FILE ] || [ $RECOMPUTE_WEIGHTS = true ] ||
		[ $UPDATED_OSM = true ] || [ $UPDATED_THERMAL_FLIGHT = true ]; then
	
		if [ -e $OSM_FILE_CROPPED ] && [ -e $THERMAL_FLIGHT_MORNING ] && 
			[ -e $THERMAL_FLIGHT_EVENING ]; then
			
			# set the --verbose flag if --debug is enabled
			if [ $DEBUG = true ]; then
				VERBOSE="--verbose"
			else 
				VERBOSE=""
			fi
			info "Compute weighted lines for $OSM_FILE_CROPPED usining $THERMAL_FLIGHT_MORNING and $THERMAL_FLIGHT_EVENING..."
			info "Note: that can take very long"
			Rscript --vanilla ./preprocessing/weightedlines.R \
				--osm_file=$OSM_FILE_CROPPED \
				--raster_morning=$THERMAL_FLIGHT_MORNING \
				--raster_evening=$THERMAL_FLIGHT_EVENING \
				--bbox_left=$BBOX_LEFT \
				--bbox_bottom=$BBOX_BOTTOM \
				--bbox_right=$BBOX_RIGHT \
				--bbox_top=$BBOX_TOP \
				--output=$WEIGHTED_LINES_FILE \
				$VERBOSE
				
			if [ ! $? ]; then 
				error "Failed to compute weighted lines" 1>&2
				rm $WEIGHTED_LINES_FILE
				exit 1
			fi
			
			info "Copy $WEIGHTED_LINES_FILE to $RESOURCES_DIR..."
			cp $WEIGHTED_LINES_FILE $RESOURCES_DIR
			
		else 
			error "Atleast one of the files $OSM_FILE_CROPPED, $THERMAL_FLIGHT_MORNING or $THERMAL_FLIGHT_EVENING is missing" 1>&2
			exit 1
		fi
	else 
		info "Weighted lines are allready up to date"
	fi
}

###########################################################
# Main:
###########################################################

parse_args "$@"
download_osm_file
download_weather_data
download_thermal_flight_files
compute_weighted_lines
