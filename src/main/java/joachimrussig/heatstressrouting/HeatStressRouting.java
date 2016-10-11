package joachimrussig.heatstressrouting;

/**
 * The main classs of the heat stress routing app.
 * 
 * @author Joachim Rußig
 */

public class HeatStressRouting {

	// TODO make the files configurable, e.g. threw an config file
	public static final String BASE_DIR = "./src/main/resources/data/";
	public static final String OSM_FILE_NAME = "karlsruhe.osm";
	public static final String WEATHER_FILE_NAME = "weather_data.csv";
	public static final String WAY_SEGMENTS_FILE_NAME = "weighted_lines.csv";

	public static final String OSM_FILE = BASE_DIR + OSM_FILE_NAME;
	public static final String WEATHER_DATA = BASE_DIR + WEATHER_FILE_NAME;
	public static final String WAY_SEGMENT_WEIGHTS = BASE_DIR
			+ WAY_SEGMENTS_FILE_NAME;

	public static final String OUT_DIR = "./out/";

	public static void main(String[] args) {

		// System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY,
		// "DEBUG");

		// Tests.runOptimalTimeEvaluation();
		// Tests.runRoutingEvaluatorSetting1();
		// Tests.runRoutingEvaluatorSetting2();

	}

}