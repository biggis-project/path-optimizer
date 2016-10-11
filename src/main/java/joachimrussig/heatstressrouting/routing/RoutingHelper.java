package joachimrussig.heatstressrouting.routing;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.PathWrapper;
import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.Weighting;
import com.graphhopper.routing.util.WeightingMap;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.shapes.GHPoint;

import joachimrussig.heatstressrouting.osmdata.OSMData;
import joachimrussig.heatstressrouting.osmdata.OSMFileReader;
import joachimrussig.heatstressrouting.routing.weighting.WeightingType;
import joachimrussig.heatstressrouting.util.Result;
import joachimrussig.heatstressrouting.util.TimeRange;
import joachimrussig.heatstressrouting.waysegments.WaySegmentParser;
import joachimrussig.heatstressrouting.waysegments.WaySegments;
import joachimrussig.heatstressrouting.weatherdata.WeatherData;
import joachimrussig.heatstressrouting.weatherdata.WeatherDataParser;
import joachimrussig.heatstressrouting.weatherdata.WeatherDataUpdater;

/**
 * Helper which makes finding an optimal route with
 * {@link HeatStressGraphHopper} easier.
 * 
 * @author Joachim Rußig
 */
public class RoutingHelper {

	private HeatStressGraphHopper hopper;

	// TODO Deprecate or remove routing methods other then route(RoutingRequest)
	private WeightingType weighting = WeightingType.SHORTEST;
	private String routingAlgorithm = AlgorithmOptions.DIJKSTRA_BI;
	private String encodingManager = EncodingManager.FOOT;
	private Locale locale = Locale.ENGLISH;

	/**
	 * 
	 * @param hopper
	 *            the {@link HeatStressGraphHopper} used for routing
	 */
	public RoutingHelper(HeatStressGraphHopper hopper) {
		this.hopper = hopper;
	}

	/**
	 * Helper method to create a {@link HeatStressGraphHopper} instance using
	 * the specified files.
	 * 
	 * @param osmFile
	 *            a OSM XML or OSM PBF file (see {@link OSMFileReader})
	 * @param weatherDataFile
	 *            a CSV file containing the data of the weather station (see
	 *            {@link WeatherDataParser})
	 * @param waySegmentsFile
	 *            a CSV file containing the weighted lines segments (see
	 *            {@link WaySegmentParser})
	 * @return a {@code HeatStressGraphHopper} instance
	 * @throws IOException
	 *             if an error occurs while reading one of the specified files
	 */
	public static HeatStressGraphHopper createHopper(File osmFile,
			File weatherDataFile, File waySegmentsFile) throws IOException {

		java.nio.file.Path ghLocation = Files
				.createTempDirectory("graph_hopper");

		OSMData osmData = new OSMFileReader().read(osmFile);
		WeatherData weatherData = new WeatherDataParser()
				.parse(weatherDataFile);
		WaySegments waySegments = new WaySegmentParser().parse(waySegmentsFile);

		HeatStressGraphHopper hopper = new HeatStressGraphHopper();
		hopper.setCHEnable(false);
		hopper.setOSMFile(osmFile.getAbsolutePath());
		hopper.setGraphHopperLocation(ghLocation.toString());
		hopper.setEncodingManager(new EncodingManager(EncodingManager.FOOT));
		hopper.setOsmData(osmData);
		hopper.setWeatherData(weatherData);
		hopper.setSegments(waySegments);
		hopper.importOrLoad();

		return hopper;

	}

	public boolean updateWeatherData(File weatherDataFile, URL zipFileUrl,
			boolean updateFile, boolean backupOldFile) throws IOException {
		Optional<WeatherData> newWeatherData = WeatherDataUpdater
				.updateWeatherData(weatherDataFile, zipFileUrl, updateFile,
						backupOldFile);
		
		if (newWeatherData.isPresent()) {
			this.hopper.setWeatherData(newWeatherData.get());
			return true;
		} else {
			return false;
		}
		
	}

	/**
	 * Executes a specified {@link RoutingRequest}.
	 * 
	 * @param request
	 *            the {@link RoutingRequest} to performe
	 * @return a instance of {@link RoutingResponse} containing the
	 *         {@link GHResponse} as well as {@link Path}s returned by
	 *         {@link HeatStressGraphHopper#routePaths(GHRequest, LocalDateTime)}
	 * 
	 * @see HeatStressGraphHopper#routePaths(GHRequest, LocalDateTime)
	 */
	public RoutingResponse route(final RoutingRequest request) {
		GHRequest req = new GHRequest(request.getStart(),
				request.getDestination())
						.setWeighting(request.getWeightingType().toString())
						.setVehicle(request.getEncodingManager())
						.setLocale(request.getLocale())
						.setAlgorithm(request.getRoutingAlgorithm());

		Pair<GHResponse, List<Path>> rsp = hopper.routePaths(req,
				request.getTime());

		return new RoutingResponse(request, req, rsp.getLeft(), rsp.getRight());

	}

	/**
	 * Finds the optimal route between {@code from} and {@code to} at time
	 * {@code time} using the specified {@link WeightingType}.
	 * 
	 * @param from
	 *            the start
	 * @param to
	 *            the destination
	 * @param time
	 *            the start time
	 * @param weighting
	 *            edge weighting to use
	 * @throws IllegalArgumentException
	 *             if time is {@code null} and the weighting type is not
	 *             {@link WeightingType.SHORTEST}
	 * @return the optimal route as a {@link PathWrapper} or the errors returned
	 *         by GraphHopper
	 */
	public Result<PathWrapper, List<Throwable>> route(GHPoint from, GHPoint to,
			LocalDateTime time, WeightingType weighting) {

		if (time == null && weighting != WeightingType.SHORTEST)
			throw new IllegalArgumentException(
					"if time is null then the weighting type must be 'WeightingType.SHORTEST'");

		GHRequest req = new GHRequest(from, to)
				.setWeighting(weighting.toString()).setVehicle(encodingManager)
				.setLocale(locale).setAlgorithm(routingAlgorithm);

		GHResponse rsp;
		if (time != null) {
			rsp = hopper.route(req, time);
		} else {
			rsp = hopper.route(req);
		}

		if (rsp.hasErrors())
			return Result.errorOf(rsp.getErrors());
		else
			return Result.okayOf(rsp.getBest());
	}

	/**
	 * Finds the optimal route between {@code from} and {@code to} at time
	 * {@code time} using the specified {@link WeightingType}.
	 * 
	 * @param from
	 *            the start
	 * @param to
	 *            the destination
	 * @param time
	 *            the start time
	 * @param weighting
	 *            edge weighting to use
	 * @return the optimal route as a {@link Path} or the errors returned by
	 *         GraphHopper
	 */
	public Result<Path, List<Throwable>> routePath(GHPoint from, GHPoint to,
			LocalDateTime time, WeightingType weightingType) {
		GHRequest req = new GHRequest(from, to)
				.setWeighting(weightingType.toString())
				.setVehicle(encodingManager).setLocale(locale)
				.setAlgorithm(routingAlgorithm);

		Pair<GHResponse, List<Path>> rsp = hopper.routePaths(req, time);

		if (rsp.getLeft().hasErrors())
			return Result.errorOf(rsp.getLeft().getErrors());
		else
			return Result.okayOf(rsp.getRight().get(0));
	}

	/**
	 * Finds the shortest route between {@code from} and {@code to}.
	 * 
	 * @param from
	 *            the start
	 * @param to
	 *            the destination
	 * @return the shortest route as a {@link Path} or the errors returned by
	 *         GraphHopper
	 */
	public Result<Path, List<Throwable>> routePathShortest(GHPoint from,
			GHPoint to) {
		GHRequest req = new GHRequest(from, to)
				.setWeighting(WeightingType.SHORTEST.toString())
				.setVehicle(encodingManager).setLocale(locale)
				.setAlgorithm(routingAlgorithm);

		Pair<GHResponse, List<Path>> rsp = hopper.routePaths(req);

		if (rsp.getLeft().hasErrors())
			return Result.errorOf(rsp.getLeft().getErrors());
		else
			return Result.okayOf(rsp.getRight().get(0));
	}

	/**
	 * Finds the optimal route between {@code from} and {@code to} at time
	 * {@code time} using the {@link WeightingType} specified in
	 * {@code this.weighting}.
	 * 
	 * @param from
	 *            the start
	 * @param to
	 *            the destination
	 * @param time
	 *            the start time
	 * @param weighting
	 *            edge weighting to use
	 * @return the optimal route as a {@link Path} or the errors returned by
	 *         GraphHopper
	 */
	public Result<Path, List<Throwable>> routePath(GHPoint from, GHPoint to,
			LocalDateTime time) {
		return routePath(from, to, time, this.weighting);
	}

	/**
	 * Finds the optimal route between {@code from} and {@code to} at time
	 * {@code time} using the {@link WeightingType} specified in
	 * {@code this.weighting}.
	 * 
	 * @param from
	 *            the start
	 * @param to
	 *            the destination
	 * @param time
	 *            the start time
	 * @param weighting
	 *            edge weighting to use
	 * @return the optimal route as a {@link PathWrapper} or the errors returned
	 *         by GraphHopper
	 */
	public Result<PathWrapper, List<Throwable>> route(GHPoint from, GHPoint to,
			LocalDateTime time) {
		return route(from, to, time, this.weighting);
	}

	/**
	 * Finds the shortest route between {@code from} and {@code to}.
	 * 
	 * @param from
	 *            the start
	 * @param to
	 *            the destination
	 * @return the shortest route as a {@link PathWrapper} or the errors
	 *         returned by GraphHopper
	 */
	public Result<PathWrapper, List<Throwable>> findShortestRoute(GHPoint from,
			GHPoint to) {
		return route(from, to, null, WeightingType.SHORTEST);
	}

	/**
	 * Computes the distance of the {@link Path} {@code path}.
	 * 
	 * @param path
	 * 
	 * @return the distance of the path
	 */
	public double routeDistance(Path path) {
		return path.calcEdges().stream()
				.mapToDouble(EdgeIteratorState::getDistance).sum();
	}

	// public double routeWeight(RoutingResponse rsp,
	// WeightingType weightingType) {
	// WeightingMap weightingMap = rsp.getGhRequest().getHints();
	// weightingMap.setWeighting(weightingType.toString());
	// FlagEncoder flagEncoder = hopper.getEncodingManager()
	// .getEncoder(rsp.getGhRequest().getVehicle());
	// Weighting weighting = hopper.createWeighting(weightingMap, flagEncoder);
	// // logger.info("class(weighting) = " + weighting.getClass().getName());
	// // logger.info("weightingMap = " + weightingMap.toString());
	// // logger.info("encoder = " + flagEncoder.toString());
	// return routeWeight(rsp.getPaths().get(0), weighting);
	// }

	/**
	 * Computes the weight of the {@link Path} {@code path} according to the
	 * specified {@link Weighting}.
	 * 
	 * @param path
	 * @param weighting
	 *            the weighting to use to compute the path weight
	 * @return the weight of the {@code path} according to the specified
	 *         {@code Weighting}
	 */
	public double routeWeight(Path path, Weighting weighting) {
		// FIXME this returns different results then
		// Pathwrapper.getBest().getRouteWeight() for weightings other then the
		// shortest path weighting
		return path.calcEdges().stream()
				.mapToDouble(e -> weighting.calcWeight(e, false, 0)).sum();
	}

	/**
	 * Computes the weight of the {@link Path} {@code path} according to the
	 * specified {@link WeightingType}.
	 * 
	 * @param path
	 * @param weightingType
	 *            the weighting type to use to compute the path weight
	 * @return the weight of the {@code path} according to the specified
	 *         {@code WeightingType}
	 */
	public double routeWeight(Path path, LocalDateTime time,
			WeightingType weightingType) {
		Weighting weighting = createWeighting(weightingType, time);
		return routeWeight(path, weighting);
	}

	/**
	 * Creates a new weighting with the specified {@code weightingType} and the
	 * {@code time}. The created Weighting can be used to calculate the route
	 * weight.
	 * 
	 * @param weightingType
	 *            the {@link WeightingType}
	 * @param time
	 *            the point in time
	 * @return a {@link Weighting} with the specified weighting and point in
	 *         time
	 */
	public Weighting createWeighting(WeightingType weightingType,
			LocalDateTime time) {
		// create weighting map and add a the time
		WeightingMap weightingMap = new WeightingMap(weightingType.toString());
		weightingMap.put("time", time.toString());

		FlagEncoder flagEncoder = new EncodingManager(encodingManager)
				.getEncoder(encodingManager);

		return hopper.createWeighting(weightingMap, flagEncoder);
	}

	public HeatStressGraphHopper getHopper() {
		return hopper;
	}

	public void setHopper(HeatStressGraphHopper hopper) {
		this.hopper = hopper;
	}

	public TimeRange<LocalDateTime> getTimeRange() {
		return this.hopper.getWeatherData().getTimeRange();
	}

	public WeightingType getWeighting() {
		return weighting;
	}

	public void setWeighting(WeightingType weighting) {
		this.weighting = weighting;
	}

	public String getRoutingAlgorithm() {
		return routingAlgorithm;
	}

	public void setRoutingAlgorithm(String routingAlgorithm) {
		this.routingAlgorithm = routingAlgorithm;
	}

	public String getEncodingManager() {
		return encodingManager;
	}

	public void setEncodingManager(String encodingManager) {
		this.encodingManager = encodingManager;
	}

	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}
}
