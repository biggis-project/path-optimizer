package joachimrussig.heatstressrouting.evaluation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.StopWatch;

import joachimrussig.heatstressrouting.waysegments.WaySegments;
import joachimrussig.heatstressrouting.weatherdata.WeatherData;
import joachimrussig.heatstressrouting.weatherdata.WeatherDataParser;
import joachimrussig.heatstressrouting.waysegments.WaySegmentParser;
import joachimrussig.heatstressrouting.osmdata.OSMData;
import joachimrussig.heatstressrouting.osmdata.OSMFileReader;
import joachimrussig.heatstressrouting.routing.HeatStressGraphHopper;

/**
 * An abstract {@class Evaluator} class that provides common functionalities,
 * e.g. to Initialize the GraphHopper or to read the required data.
 * 
 * @author Joachim Rußig
 */
public abstract class Evaluator {

	public static final String SHOP = "shop";

	public static final String AMENITY = "amenity";

	public static final String DELIMITER = "|";

	public static final String DELIMITER_POINT = ",";

	public static final String DELIMITER_POINT_LIST = ";";

	protected File weatherDataFile;

	protected File osmFile;
	protected File waySegmentsFile = null;
	protected java.nio.file.Path ghLocation = null;
	protected String routingAlgo = AlgorithmOptions.DIJKSTRA;
	protected WeatherData weatherData;

	protected OSMData osmData;
	protected WaySegments waySegments = null;
	protected HeatStressGraphHopper hopper = null;

	protected Long seed;
	protected Random rnd = new Random();

	private Logger logger = LoggerFactory.getLogger(Evaluator.class);
	protected boolean exportResults = false;
	protected File outFile = null;

	public Evaluator() {
	}

	/**
	 * Initializes the evaluator and e.g. loads the GrphHopper and the required
	 * data.
	 * 
	 * @throws IOException
	 *             if an error occurred while reading a file
	 */
	public abstract void init() throws IOException;

	/**
	 * Runs the evaluation.
	 * 
	 * @throws IOException
	 *             e.g. if {@code init()} called at first
	 */
	public abstract void run() throws IOException;

	public void exportResultsTo(File outFile) {
		this.exportResults = outFile != null;
		this.outFile = outFile;
	}

	public WaySegments getWaySegments() {
		return waySegments;
	}

	public File getWaySegmentsFile() {
		return waySegmentsFile;
	}

	public Path getGhLocation() {
		return ghLocation;
	}

	public HeatStressGraphHopper getHopper() {
		return hopper;
	}

	public OSMData getOsmData() {
		return osmData;
	}

	public File getOsmFile() {
		return osmFile;
	}

	public Optional<File> getOutFile() {
		return Optional.ofNullable(outFile);
	}

	public Random getRnd() {
		return rnd;
	}

	public String getRoutingAlgo() {
		return routingAlgo;
	}

	public Long getSeed() {
		return seed;
	}

	public WeatherData getWeatherData() {
		return weatherData;
	}

	public File getWeatherDataFile() {
		return weatherDataFile;
	}

	public boolean isExportResults() {
		return exportResults;
	}

	protected void loadEdgeSegments() throws IOException {
		if (waySegmentsFile == null)
			throw new IllegalStateException("no edgeSegmentsFile specified");

		logger.info("read edge segments (file = "
				+ waySegmentsFile.getAbsolutePath() + ")...");
		StopWatch sw2 = new StopWatch().start();
		WaySegments segments = new WaySegmentParser()
				.parse(this.waySegmentsFile);
		this.waySegments = segments;

		logger.info("done (" + waySegments.getSegments().size() + " segments, "
				+ sw2.stop().getTime() + " ms )");
	}

	protected void loadHeatStressGraphHopper() {
		logger.info(
				"GraphHopper Location is " + this.ghLocation.toAbsolutePath());

		HeatStressGraphHopper hopper = new HeatStressGraphHopper();
		hopper.setCHEnable(false);
		hopper.setOSMFile(osmFile.getAbsolutePath());
		hopper.setGraphHopperLocation(this.ghLocation.toString());
		hopper.setEncodingManager(new EncodingManager(EncodingManager.FOOT));
		hopper.setOsmData(this.osmData);
		hopper.setWeatherData(this.weatherData);
		if (waySegments != null)
			hopper.setSegments(this.waySegments);
		hopper.importOrLoad();

		logger.info("CHEnabled = " + hopper.isCHEnabled() + ", getNodes = "
				+ hopper.getGraphHopperStorage().getNodes() + ", mainEdges = "
				+ hopper.getGraphHopperStorage().getAllEdges().getMaxId());

		this.hopper = hopper;
	}

	protected void loadOSMData() throws IOException {
		logger.info(
				"read OSM file (file = " + osmFile.getAbsolutePath() + ")...");

		StopWatch sw = new StopWatch().start();
		OSMData osmData = new OSMFileReader().read(osmFile);

		logger.info("done (" + sw.stop().getTime() + " ms, "
				+ osmData.getEntities().size() + " Entities)");

		this.osmData = osmData;
	}

	protected void loadWeatherData() throws IOException {
		logger.info("read weather records (file = "
				+ weatherDataFile.getAbsolutePath() + ")...");

		StopWatch sw = new StopWatch().start();
		WeatherData weatherData = new WeatherDataParser()
				.parse(weatherDataFile);

		logger.info("done (" + sw.stop().getTime() + " ms, "
				+ weatherData.getWeatherRecords().size() + " records, "
				+ "time range: "
				+ weatherData.getWeatherRecords().firstKey().toLocalDate()
				+ " - "
				+ weatherData.getWeatherRecords().lastKey().toLocalDate()
				+ ")");

		this.weatherData = weatherData;
	}

	public void setWaySegments(WaySegments waySegments) {
		this.waySegments = waySegments;
	}

	public void setWaySegmentsFile(File waySegmentsFile) {
		this.waySegmentsFile = waySegmentsFile;
	}

	public void setExportResults(boolean exportResults) {
		this.exportResults = exportResults;
	}

	public void setGhLocation(Path ghLocation) {
		this.ghLocation = ghLocation;
	}

	public void setHopper(HeatStressGraphHopper hopper) {
		this.hopper = hopper;
	}

	public void setOsmData(OSMData osmData) {
		this.osmData = osmData;
	}

	public void setOsmFile(File osmFile) {
		this.osmFile = osmFile;
	}

	public void setOutFile(File outFile) {
		this.outFile = outFile;
	}

	public void setRnd(Random rnd) {
		this.rnd = rnd;
	}

	public void setRoutingAlgo(String routingAlgo) {
		this.routingAlgo = routingAlgo;
	}

	public void setSeed(Long seed) {
		this.seed = seed;
	}

	public void setWeatherData(WeatherData weatherData) {
		this.weatherData = weatherData;
	}

	public void setWeatherDataFile(File weatherDataFile) {
		this.weatherDataFile = weatherDataFile;
	}

}
