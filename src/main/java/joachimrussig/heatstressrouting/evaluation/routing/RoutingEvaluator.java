package joachimrussig.heatstressrouting.evaluation.routing;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.imageio.IIOException;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.PathWrapper;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.Weighting;
import com.graphhopper.routing.util.WeightingMap;
import com.graphhopper.util.shapes.GHPoint;

import joachimrussig.heatstressrouting.evaluation.Evaluator;
import joachimrussig.heatstressrouting.osmdata.OSMUtils;
import joachimrussig.heatstressrouting.routing.HeatStressGraphHopper;
import joachimrussig.heatstressrouting.routing.weighting.WeightingType;
import joachimrussig.heatstressrouting.util.Utils;
import joachimrussig.heatstressrouting.waysegments.WaySegments;

/**
 * A class to evaluate the heat stress routing.
 * 
 * @author Joachim Rußig
 */
public class RoutingEvaluator extends Evaluator {

	private final Logger logger = LoggerFactory
			.getLogger(RoutingEvaluator.class);

	private boolean initialized = false;
	private boolean forceReinitialisation = false;

	private List<RoutingResultRecord> resultRecords = null;

	private List<Node> starts = null;
	private List<Node> destinations = null;
	private List<LocalDate> dates = null;
	private List<LocalTime> timePoints = null;
	private double weightDistance = 0.5;
	private double weightThermalComfort = 0.5;

	private String encoder = EncodingManager.FOOT;

	public RoutingEvaluator() {
		this.hopper = null;
	}

	public void init() throws IOException {

		logger.info("start initialisation");
		StopWatch sw = new StopWatch();
		sw.start();

		if (ghLocation == null)
			this.ghLocation = Files.createTempDirectory("graph_hopper");

		if (osmFile == null)
			throw new IllegalStateException("no OSM file specified");

		if (weatherDataFile == null)
			throw new IllegalStateException("no weather data file specified");

		if (waySegmentsFile == null)
			throw new IIOException("no edge segment file specified");

		loadOSMData();
		loadWeatherData();
		loadEdgeSegments();
		loadHeatStressGraphHopper();

		hopper.setWeights(this.weightDistance, this.weightThermalComfort);

		this.resultRecords = new ArrayList<>();

		if (timePoints == null) {
			timePoints = new ArrayList<>(2);
			timePoints.add(LocalTime.of(11, 0));
			timePoints.add(LocalTime.of(19, 0));
		}

		this.initialized = true;

		sw.stop();
		logger.info("finished initialisation in "
				+ Utils.formatDurationMills(sw.getTime()) + " ms");

	}

	@Override
	public void run() throws IOException {

		if (!initialized || forceReinitialisation)
			init();

		if (starts == null || destinations == null)
			throw new IllegalStateException("no start or destination set");

		if (starts.size() != destinations.size())
			throw new IllegalStateException(
					"'starts' and 'destinations' must have the same length");

		if (dates == null || dates.isEmpty())
			throw new IllegalStateException(
					"at least one date must be specified");

		if (timePoints == null || timePoints.isEmpty())
			throw new IllegalStateException(
					"at least one timePoint must be specified");

		if (outFile != null && outFile.exists()) {
			PrintWriter writer = new PrintWriter(outFile);
			writer.print("");
			writer.close();
		}

		System.out.println("Starting evaluation with " + starts.size()
				+ " starts/destinations " + ", " + dates.size() + " dates and "
				+ timePoints.size() + " time points. Routing algorithm is "
				+ routingAlgo + "...");
		StopWatch sw = new StopWatch();
		sw.start();

		int recordId = 0;
		for (int i = 0; i < starts.size(); i++) {

			List<RoutingResultRecord> results = new ArrayList<>();

			Node fromNode = starts.get(i);
			Node toNode = destinations.get(i);

			GHPoint from = new GHPoint(fromNode.getLatitude(),
					fromNode.getLongitude());
			GHPoint to = new GHPoint(toNode.getLatitude(),
					toNode.getLongitude());

			String idx = String.format("%02d: ", i);
			System.out.println(
					idx + "Routing from " + from + " (" + fromNode.getId()
							+ ") to " + to + " (" + toNode.getId() + "):");

			for (LocalDate date : this.dates) {
				for (LocalTime t : this.timePoints) {
					LocalDateTime time = LocalDateTime.of(date, t);
					// this.hopper.setTimePoint(time);

					System.out.println(
							"Datetime: " + time + ", iteration = " + i);

					Optional<RoutingResultRecord> resShortest = doRouting(
							recordId, i, WeightingType.SHORTEST, fromNode,
							toNode, from, to, time);
					Optional<RoutingResultRecord> resTemperature = doRouting(
							recordId, i, WeightingType.TEMPERATURE, fromNode,
							toNode, from, to, time);
					Optional<RoutingResultRecord> resHeatIndex = doRouting(
							recordId, i, WeightingType.HEAT_INDEX, fromNode,
							toNode, from, to, time);
					Optional<RoutingResultRecord> resHeatIndexWeighted = doRouting(
							recordId, i, WeightingType.HEAT_INDEX_WEIGHTED,
							fromNode, toNode, from, to, time);

					if (resShortest.isPresent() && resTemperature.isPresent()
							&& resHeatIndex.isPresent()
							&& resHeatIndexWeighted.isPresent()) {
						results.add(resShortest.get());
						results.add(resTemperature.get());
						results.add(resHeatIndex.get());
						results.add(resHeatIndexWeighted.get());
					}

					recordId++;

				}

			}
			System.out.println();

			if (exportResults) {
				if (this.outFile != null) {
					writeResults(results, this.outFile, true);
				} else {
					throw new IllegalStateException(
							"if exportResults is true a outFile must be specified");
				}
			}

			this.resultRecords.addAll(results);

		}

		if (exportResults) {
			if (this.outFile != null) {
				writeResults(this.resultRecords, this.outFile, false);
			} else {
				throw new IllegalArgumentException(
						"if exportResults is true a outFile must be specified");
			}
		}

		sw.stop();
		System.out.println("finished evaluation in "
				+ Utils.formatDurationMills(sw.getTime()));

	}

	private Optional<RoutingResultRecord> doRouting(int recordId, int i,
			WeightingType method, Node fromNode, Node toNode, GHPoint from,
			GHPoint to, LocalDateTime time) {

		if (method == WeightingType.SHORTEST) {
			System.out.print("Shortest Path Routing... ");
		} else if (method == WeightingType.TEMPERATURE) {
			System.out.print("Minimum Temperature Routing... ");
		} else if (method == WeightingType.HEAT_INDEX) {
			System.out.print("Minimum Heat Index Routing... ");
		} else if (method == WeightingType.HEAT_INDEX_WEIGHTED) {
			System.out.print("Weighted Minimum Heat Index Routing... ");
		} else {
			throw new IllegalStateException(
					"unsupported weighting method '" + method + "'");
		}

		StopWatch sw = new StopWatch();
		sw.start();

		GHRequest reqShortest = new GHRequest(from, to)
				.setWeighting(method.toString())
				.setVehicle(EncodingManager.FOOT).setLocale(Locale.GERMAN)
				.setAlgorithm(routingAlgo);

		Pair<GHResponse, List<Path>> resShortest = this.hopper
				.routePaths(reqShortest, time);
		sw.stop();
		System.out.println("done (" + sw.getTime() + " ms)");

		GHResponse rsp = resShortest.getLeft();

		if (rsp.hasErrors()) {
			logger.error("rsp " + method.toString() + " hasErros = "
					+ rsp.getErrors());
			logger.debug("Errors: ");
			rsp.getErrors().forEach(Throwable::getMessage);
			rsp.getErrors().forEach(Throwable::printStackTrace);
			return Optional.empty();
		}
		PathWrapper pathWrapper = rsp.getBest();
		Path path = resShortest.getRight().get(0);
		double dist = pathWrapper.getDistance();
		double costsTemp = routeCostsTemperature(path, time);
		double costsHeatIndex = routeCostsHeatIndex(path, time);
		Duration duration = Duration.ofMillis(pathWrapper.getTime());
		System.out.println("\tDistance: " + dist + ", costsTemperature: "
				+ costsTemp + ", costsHeatIndex: " + costsHeatIndex
				+ ", Duration: " + Utils.formatDuration(duration));

		return Optional.of(new RoutingResultRecord(recordId, i, time,
				method.toString(), fromNode, toNode, dist, costsTemp,
				costsHeatIndex, duration.toMillis(), pathWrapper.getPoints()));
	}


	private double routeCostsTemperature(Path path, LocalDateTime time) {
		return routeCosts(path,
				createWeighting(WeightingType.TEMPERATURE, time));
	}

	private double routeCostsHeatIndex(Path path, LocalDateTime time) {

		return routeCosts(path,
				createWeighting(WeightingType.HEAT_INDEX, time));
	}
	
	private double routeCosts(Path path, Weighting weighting) {
		return path.calcEdges().stream()
				.mapToDouble(e -> weighting.calcWeight(e, false, 0)).sum();
	}

	private Weighting createWeighting(WeightingType weightingType,
			LocalDateTime time) {

		WeightingMap weightingMap = new WeightingMap(weightingType.toString())
				.put("time", time.toString());

		FlagEncoder encoder = hopper.getEncodingManager()
				.getEncoder(this.encoder);

		return hopper.createWeighting(weightingMap, encoder);
	}

	public void randomStartsAndDestinations(int n) {
		randomStartsAndDestinations(n, 0);
	}

	/**
	 * Generates a random list of {@code n} start-destination pairs of OSM
	 * nodes.
	 *
	 * @param n
	 *            number of start-destinations pairs
	 * @param minDistance
	 *            minimal distance between start and destination in meter
	 */
	public void randomStartsAndDestinations(int n, double minDistance) {
		if (minDistance < 0)
			throw new IllegalArgumentException("minDistance must be positive");

		if (!initialized)
			throw new IllegalStateException(
					"not initialized! call init() first!");

		if (seed != null)
			rnd.setSeed(seed);

		System.out.println("init starts and destination with random nodes");

		ArrayList<Node> starts = new ArrayList<>(n);
		ArrayList<Node> destinations = new ArrayList<>(n);

		List<Node> nodes = osmData.getNodes().stream()
				.collect(Collectors.toList());
		int m = nodes.size();

		int i = 0;
		while (i < n) {
			Node s = nodes.get(rnd.nextInt(m));
			Node d = nodes.get(rnd.nextInt(m));
			if (s.equals(d) || OSMUtils.distance(s, d) < minDistance)
				continue;
			starts.add(s);
			destinations.add(d);
			i++;
		}

		this.starts = starts;
		this.destinations = destinations;
	}

	public void randomDates(int n) {
		randomDates(n, null, null, null);
	}

	public void randomDates(int n, LocalDate from, LocalDate to) {
		randomDates(n, from, to, null);
	}

	public void randomDates(int n, LocalDate from, LocalDate to,
			Double minTemperature) {
		LocalDateTime fromTime = weatherData.getWeatherRecords().firstKey();
		LocalDateTime toTime = weatherData.getWeatherRecords().lastKey();

		if (from != null) {
			LocalDateTime t = LocalDateTime.of(from, LocalTime.MIN);
			if (!weatherData.inTimeRange(t))
				throw new IllegalArgumentException(
						"from is not in time range of the weather records");
			fromTime = t;
		}

		if (to != null) {
			LocalDateTime t = LocalDateTime.of(to, LocalTime.MAX);
			if (!weatherData.inTimeRange(t))
				throw new IllegalArgumentException(
						"to is not in time range of the weather records");
			toTime = t;
		}

		double minTempDiff = Math.abs(waySegments.getMinTemperatureDifference()
				.orElseThrow(() -> new IllegalStateException(
						"no minimal temperature difference")));
		System.out.println("minTempDiff = " + minTempDiff);
		double minTemp;
		if (minTemperature != null)
			minTemp = Math.max(minTemperature, minTempDiff);
		else
			minTemp = minTempDiff;

		if (seed != null)
			rnd.setSeed(seed);

		List<LocalDateTime> times = weatherData.getWeatherRecords()
				.subMap(fromTime, true, toTime, true).navigableKeySet().stream()
				.collect(Collectors.toList());

		int m = times.size();

		logger.debug("n = " + n + ", fromTime = " + fromTime + ", toTime = "
				+ toTime);

		ArrayList<LocalDate> dates = new ArrayList<>(n);

		// assumption: weatherRecords containing 24 values for each day
		int i = 0;
		while (i < n) {
			LocalDate date = times.get(rnd.nextInt(m)).toLocalDate();
			if (dates.contains(date))
				continue;
			if (!Utils.allTempsGreaterEqMinTemp(weatherData, date, minTemp))
				continue;
			dates.add(date);
			i++;
		}
		this.dates = dates;

	}

	public void randomTimePoints(int n) {
		randomTimePoints(n, null, null, false);
	}

	/**
	 * Generates a random list of points in time of length <code>n</code>. If
	 * <code>from</code> or <code>to</code> are <code>null</code> the bounds ar
	 * set to <code>LocalTime.MIN</code> respectively <code>LocalTime.MAX</code>
	 * .
	 *
	 * @param n
	 *            number of points in time
	 * @param from
	 *            lower bound (can be null)
	 * @param to
	 *            upper bound (can be null)
	 * @param add
	 *            should the points in time added to the current list or should
	 *            should the current list be replaced
	 */
	public void randomTimePoints(int n, LocalTime from, LocalTime to,
			boolean add) {
		int min;
		if (from != null)
			min = from.toSecondOfDay();
		else
			min = LocalTime.MIN.toSecondOfDay();

		int max;
		if (to != null)
			max = to.toSecondOfDay();
		else
			max = LocalTime.MAX.toSecondOfDay();

		if (seed != null)
			rnd.setSeed(seed);

		ArrayList<LocalTime> localTimes = new ArrayList<>(n);
		int i = 0;
		while (i < n) {
			int time = rnd.nextInt(max);
			if (time < min)
				continue;
			localTimes.add(LocalTime.ofSecondOfDay(time));
			i++;
		}
		if (add)
			this.timePoints.addAll(localTimes);
		else
			this.timePoints = localTimes;
	}

	/**
	 * Initializes the <code>timePoints</code> with a sequential list of points
	 * in time in the range <code>from</code>-<code>to</code>, in steps of
	 * <code>timeStep</code> minutes.
	 *
	 * @param from
	 *            first point in time (inclusive)
	 * @param to
	 *            last point in time (inclusive)
	 * @param timeStep
	 *            in minutes
	 */
	public void sequentialTimePoints(LocalTime from, LocalTime to,
			long timeStep) {
		if (!from.isBefore(to))
			throw new IllegalArgumentException("'from' must be before 'to'");

		if (timeStep <= 0)
			throw new IllegalArgumentException("'timeStep' must be positive");

		// http://stackoverflow.com/questions/7139382/java-rounding-up-to-an-int-using-math-ceil
		final long n = ChronoUnit.MINUTES.between(from, to);

		List<LocalTime> localTimes = new ArrayList<>();
		long i = 0;
		while (i <= n) {
			localTimes.add(from);
			from = from.plusMinutes(timeStep);
			i += timeStep;
		}
		this.timePoints = localTimes;
	}

	private void writeResults(List<RoutingResultRecord> resultRecords,
			File outFile, boolean append) throws IOException {

		logger.info("write " + resultRecords.size() + " result(s) to "
				+ outFile.getAbsolutePath() + ", append = " + append
				+ ", exists = " + outFile.exists() + ", file size = "
				+ outFile.length());

		FileWriter writer = new FileWriter(outFile, append);

		if (!append || !outFile.exists() || outFile.length() == 0) {
			// String header = String.join(DELIMITER, "iteration", "time",
			// "weighting.method", "from", "to",
			// "from.lat", "from.lon", "to.lat", "to.lon", "dist",
			// "cost.temperature", "cost.heatindex",
			// "duration",
			// "lat", "lon");
			String header = String.join(DELIMITER, "rowid", "iteration", "time",
					"weighting.method", "from", "to", "from.lat", "from.lon",
					"to.lat", "to.lon", "dist", "cost.temperature",
					"cost.heatindex", "duration", "points");
			writer.append(header);
		}

		for (RoutingResultRecord rec : resultRecords) {
			writer.append("\n");
			writer.append(rec.toCsvRecord());
		}

		writer.flush();
		writer.close();
	}

	public HeatStressGraphHopper getHopperAggregated() {
		return hopper;
	}

	public void setHopperAggregated(HeatStressGraphHopper hopper) {
		this.hopper = hopper;
	}

	public java.nio.file.Path getGhLocation() {
		return ghLocation;
	}

	public void setGhLocation(java.nio.file.Path ghLocation) {
		this.ghLocation = ghLocation;
	}

	public boolean isForceReinitialisation() {
		return forceReinitialisation;
	}

	public void setForceReinitialisation(boolean forceReinitialisation) {
		this.forceReinitialisation = forceReinitialisation;
	}

	public File getWaySegmentsFile() {
		return waySegmentsFile;
	}

	public void setWaySegmentsFile(File waySegmentsFile) {
		this.waySegmentsFile = waySegmentsFile;
	}

	public WaySegments getWaySegments() {
		return waySegments;
	}

	public void setWaySegments(WaySegments edgeSegments) {
		this.waySegments = edgeSegments;
	}

	public List<Node> getStarts() {
		return starts;
	}

	public void setStarts(List<Node> starts) {
		this.starts = starts;
	}

	public List<Node> getDestinations() {
		return destinations;
	}

	public void setDestinations(List<Node> destinations) {
		this.destinations = destinations;
	}

	public List<LocalDate> getDates() {
		return dates;
	}

	public void setDates(List<LocalDate> dates) {
		this.dates = dates;
	}

	public boolean isExportResults() {
		return exportResults;
	}

	public void setExportResults(boolean exportResults) {
		this.exportResults = exportResults;
	}

	public void setOutFile(File outFile) {
		this.outFile = outFile;
	}

	public List<RoutingResultRecord> getResultRecords() {
		return resultRecords;
	}

	public void setResultRecords(List<RoutingResultRecord> resultRecords) {
		this.resultRecords = resultRecords;
	}

	public List<LocalTime> getTimePoints() {
		return timePoints;
	}

	public void setTimePoints(List<LocalTime> timePoints) {
		this.timePoints = timePoints;
	}

	public String getRoutingAlgo() {
		return routingAlgo;
	}

	public void setRoutingAlgo(String routingAlgo) {
		this.routingAlgo = routingAlgo;
	}

	public double getWeightDistance() {
		return weightDistance;
	}

	public void setWeightDistance(double weightDistance) {
		this.weightDistance = weightDistance;
	}

	public void setWeights(double weightDistance, double weightThermalComfort) {
		this.weightDistance = weightDistance;
		this.weightThermalComfort = weightThermalComfort;
	}

	public double getWeightThermalComfort() {
		return weightThermalComfort;
	}

	public void setWeightThermalComfort(double weightThermalComfort) {
		this.weightThermalComfort = weightThermalComfort;
	}

	public String getEncoder() {
		return encoder;
	}

	public void setEncoder(String encoder) {
		this.encoder = encoder;
	}
}
