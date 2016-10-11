package joachimrussig.heatstressrouting.evaluation.optimaltime;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.StopWatch;
import org.jooq.lambda.Seq;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;
import com.graphhopper.routing.Path;
import com.graphhopper.util.shapes.GHPoint;

import joachimrussig.heatstressrouting.evaluation.Evaluator;
import joachimrussig.heatstressrouting.optimaltime.finder.ObjectiveFunctionPathImpl;
import joachimrussig.heatstressrouting.optimaltime.finder.OptimalTimeFinder;
import joachimrussig.heatstressrouting.optimaltime.finder.OptimalTimeFinderHeuristic;
import joachimrussig.heatstressrouting.optimaltime.finder.RoutingObjectiveFunction;
import joachimrussig.heatstressrouting.optimaltime.finder.SimpleObjectiveFunction;
import joachimrussig.heatstressrouting.optimaltime.nearbysearch.NearbySearch;
import joachimrussig.heatstressrouting.optimaltime.nearbysearch.NearbySearchResult;
import joachimrussig.heatstressrouting.optimaltime.nearbysearch.ScoreFunction;
import joachimrussig.heatstressrouting.optimaltime.nearbysearch.ThermalComfortScoreFunction;
import joachimrussig.heatstressrouting.optimaltime.nearbysearch.WeightedSumScoreFunction;
import joachimrussig.heatstressrouting.osmdata.EntityFilter;
import joachimrussig.heatstressrouting.osmdata.OSMOpeningHours;
import joachimrussig.heatstressrouting.osmdata.OSMUtils;
import joachimrussig.heatstressrouting.routing.HeatStressGraphHopper;
import joachimrussig.heatstressrouting.routing.RoutingHelper;
import joachimrussig.heatstressrouting.routing.weighting.WeightingType;
import joachimrussig.heatstressrouting.thermalcomfort.HeatIndex;
import joachimrussig.heatstressrouting.thermalcomfort.ThermalComfortHeatIndex;
import joachimrussig.heatstressrouting.thermalcomfort.ThermalComfortTemperature;
import joachimrussig.heatstressrouting.util.TimeRange;
import joachimrussig.heatstressrouting.util.Utils;

/**
 * A class to evaluate the search for an optimal point in time.
 * 
 * @author Joachim Rußig
 */
public class OptimalTimeEvaluator extends Evaluator {

	public static final Duration DEFAULT_TIME_BUFFER = Duration.ofMinutes(15);
	static final String[] SHOP_TAGS = { "bakery", "beverages", "butcher",
			"cheese", "department_store", "general", "kiosk", "mall",
			"supermarket", "chemist", "medical_supply" };
	static final String[] AMENITY_TAGS = { "biergarten", "cafe",
			"drinking_water", "fast_food", "ice_cream", "pub", "restaurant",
			"atm", "bank" };
	static Logger logger = LoggerFactory.getLogger(OptimalTimeEvaluator.class);

	private List<Tag> targetTags;

	private List<OptimalTimeEvaluationItem> evaluationItems;

	private List<OptimalTimeResultRecord> results = null;
	private Long executionTime = null;

	private OptimalTimeFinder optimalTimeFinder;

	private boolean initialized = false;

	private int nearbySearchMaxResults = 20;
	private Double maxDistance = null;

	public OptimalTimeEvaluator() {
	}

	@Override
	public void init() throws IOException {

		if (osmFile == null)
			throw new IllegalStateException("no OSM file specified");

		if (weatherDataFile == null)
			throw new IllegalStateException("no weather data file specified");

		if (ghLocation == null)
			ghLocation = Files.createTempDirectory("graph_hopper");

		loadOSMData();
		loadWeatherData();
		loadEdgeSegments();
		loadHeatStressGraphHopper();

		initialized = true;
	}

	/**
	 * Runs the evaluation.
	 * 
	 * @throws IllegalStateException
	 *             if the evaluator has not be initialized at first
	 */
	@Override
	public void run() throws IOException {

		if (!initialized)
			throw new IllegalStateException(
					"not yet initialized! Call init() first!");

		if (evaluationItems == null || evaluationItems.size() == 0)
			throw new IllegalStateException("no evaluationItesm specified!");

		List<OptimalTimeResultRecord> resultRecords = new ArrayList<>(
				evaluationItems.size());

		StopWatch sw = new StopWatch();
		sw.start();

		this.executionTime = 0L;

		int noItem = 1;
		for (OptimalTimeEvaluationItem item : evaluationItems) {
			GHPoint start = new GHPoint(item.getStart().getLatitude(),
					item.getStart().getLongitude());

			System.out.println();
			System.out.println("item " + noItem + " of "
					+ evaluationItems.size() + ": Start (" + start + ") at "
					+ item.now + ": " + item.toString());

			StopWatch sw2 = new StopWatch();
			sw2.start();

			logger.debug("nearbySearchMaxResults = " + nearbySearchMaxResults
					+ ", maxDistance = " + maxDistance);

			OptimalTimeResultRecord res = new OptimalTimeResultRecord(item);

			List<OptimalTimeResultItem> referenceResultItems = getReferenceResultItem(
					item);
			if (!referenceResultItems.isEmpty()) {
				System.out.println("Nearest: " + referenceResultItems.get(0));
				res.addResults(referenceResultItems);
			} else {
				System.out.println(
						"Not Opened at arrivalTime = " + item.getNow());
			}

			OptimalTimeFinder optTimeFinderTemp = new OptimalTimeFinder(
					new SimpleObjectiveFunction(
							new ThermalComfortTemperature(weatherData)),
					hopper);
			List<OptimalTimeResultItem> optTimeTempResultItems = getOptimalResultItem(
					item, optTimeFinderTemp, new WeightedSumScoreFunction(),
					OptimalTimeResultType.TEMPERATURE);
			if (!optTimeTempResultItems.isEmpty()) {
				System.out.println("Optimal Temperature: "
						+ optTimeTempResultItems.get(0));
				res.addResults(optTimeTempResultItems);
			} else {
				System.out.println("Optimal Temperature: None");
			}

			OptimalTimeFinder optTimeFinderHI = new OptimalTimeFinder(
					new SimpleObjectiveFunction(
							new ThermalComfortHeatIndex(weatherData)),
					hopper);
			List<OptimalTimeResultItem> optTimeHIResultItems = getOptimalResultItem(
					item, optTimeFinderHI, new WeightedSumScoreFunction(),
					OptimalTimeResultType.HEATINDEX);
			if (!optTimeHIResultItems.isEmpty()) {
				System.out.println(
						"Optimal HeatIndex: " + optTimeHIResultItems.get(0));
				res.addResults(optTimeHIResultItems);
			} else {
				System.out.println("Optimal HeatIndex: None");
			}

			Stopwatch stopwatch = Stopwatch.createStarted();
			OptimalTimeFinder optTimeFinderRoutingTemp = new OptimalTimeFinder(
					new RoutingObjectiveFunction(
							new ThermalComfortTemperature(weatherData), hopper,
							WeightingType.TEMPERATURE),
					hopper);
			List<OptimalTimeResultItem> optRoutingTempResultItems = getOptimalResultItem(
					item, optTimeFinderRoutingTemp,
					new ThermalComfortScoreFunction(),
					OptimalTimeResultType.ROUTING_TEMPERATURE);
			if (!optRoutingTempResultItems.isEmpty()) {
				System.out.println("Optimal TemperatureRouting: "
						+ optRoutingTempResultItems.get(0));
				res.addResults(optRoutingTempResultItems);
			} else {
				System.out.println("Optimal TemperatureRouting: None");
			}
			System.out.println("Optimal TemperatureRouting: "
					+ stopwatch.stop().toString());
			stopwatch = Stopwatch.createStarted();
			// OptimalTimeFinderHeuristic
			OptimalTimeFinderHeuristic optTimeFinderRoutingTempHeur = new OptimalTimeFinderHeuristic(
					new ObjectiveFunctionPathImpl(WeightingType.TEMPERATURE,
							new RoutingHelper(hopper)),
					WeightingType.TEMPERATURE, hopper);
			List<OptimalTimeResultItem> optRoutingTempResultItemsHeur = getOptimalResultItem(
					item, optTimeFinderRoutingTempHeur,
					new ThermalComfortScoreFunction(),
					OptimalTimeResultType.ROUTING_TEMPERATURE);
			if (!optRoutingTempResultItemsHeur.isEmpty()) {
				System.out.println("Optimal TemperatureRoutingHeur: "
						+ optRoutingTempResultItemsHeur.get(0));
				res.addResults(optRoutingTempResultItemsHeur);
			} else {
				System.out.println("Optimal TemperatureRoutingHeur: None");
			}
			System.out.println("Optimal TemperatureRoutingHeur: "
					+ stopwatch.stop().toString());

			stopwatch = Stopwatch.createStarted();
			OptimalTimeFinder optTimeFinderRoutingHI = new OptimalTimeFinder(
					new RoutingObjectiveFunction(
							new ThermalComfortTemperature(weatherData), hopper,
							WeightingType.HEAT_INDEX),
					hopper);
			List<OptimalTimeResultItem> optRoutingHIResultItems = getOptimalResultItem(
					item, optTimeFinderRoutingHI,
					new ThermalComfortScoreFunction(),
					OptimalTimeResultType.ROUTING_HEATINDEX);
			if (!optRoutingHIResultItems.isEmpty()) {
				System.out.println("Optimal HeatIndexRouting: "
						+ optRoutingHIResultItems.get(0));
				res.addResults(optRoutingHIResultItems);
			} else {
				System.out.println("Optimal HeatIndexRouting: None");
			}
			System.out.println(
					"Optimal HeatIndexRouting: " + stopwatch.stop().toString());
			stopwatch = Stopwatch.createStarted();
			// OptimalTimeFinderHeuristic
			OptimalTimeFinderHeuristic optTimeFinderRoutingHIHeur = new OptimalTimeFinderHeuristic(
					new ObjectiveFunctionPathImpl(WeightingType.HEAT_INDEX,
							new RoutingHelper(hopper)),
					WeightingType.HEAT_INDEX, hopper);
			List<OptimalTimeResultItem> optRoutingHIResultItemsHeur = getOptimalResultItem(
					item, optTimeFinderRoutingHIHeur,
					new ThermalComfortScoreFunction(),
					OptimalTimeResultType.ROUTING_HEATINDEX);
			if (!optRoutingHIResultItemsHeur.isEmpty()) {
				System.out.println("Optimal HeatindexRoutingHeur: "
						+ optRoutingHIResultItemsHeur.get(0));
				res.addResults(optRoutingHIResultItemsHeur);
			} else {
				System.out.println("Optimal HeatindexRoutingHeur: None");
			}
			System.out.println("Optimal HeatIndexRoutingHeur: "
					+ stopwatch.stop().toString());

			sw2.stop();
			System.out.println("Evaluated item " + noItem + " in "
					+ Utils.formatDurationMills(sw2.getTime()));

			resultRecords.add(res);
			executionTime += sw2.getTime();
			noItem++;
		}
		this.results = resultRecords;

		if (exportResults) {
			writeResults(resultRecords, outFile);
		}

		sw.stop();
		System.out.println("Evaluated " + evaluationItems.size() + " items in "
				+ Utils.formatDurationMills(sw.getTime()) + " and created "
				+ resultRecords.size() + " result records");
	}

	private List<OptimalTimeResultItem> getOptimalResultItem(
			OptimalTimeEvaluationItem item, OptimalTimeFinder finder,
			ScoreFunction scoreFunction, OptimalTimeResultType resultType) {

		NearbySearch nearbySearch = new NearbySearch(finder, scoreFunction);
		nearbySearch.setMaxDistance(maxDistance);

		Predicate<Entity> nodeFilter = EntityFilter
				.containsAnyTag(item.getTargetTags())
				.and(EntityFilter::hasOpeningHours);

		GHPoint start = OSMUtils.getGHPoint(item.start);

		if (item.hasEarliestTime() && item.hasLatestTime()) {
			finder.setEarliestTime(item.getEarliestTime().get());
			finder.setLatestTime(item.getLatestTime().get());
		} else {
			finder.setEarliestTime(null);
			finder.setLatestTime(null);
		}
		if (item.hasTimeBuffer()) {
			finder.setTimeBuffer(item.getTimeBuffer().get());
		} else {
			finder.setTimeBuffer(DEFAULT_TIME_BUFFER);
		}

		int maxResults = item.maxResults != null ? item.maxResults
				: this.getNearbySearchMaxResults();
		List<NearbySearchResult> results = nearbySearch.find(start, nodeFilter,
				item.now, maxResults);

		RoutingHelper helper = finder.getRoutingHelper();

		List<OptimalTimeResultItem> resultItems = new ArrayList<>(
				results.size());

		for (NearbySearchResult res : results) {

			double temp = weatherData.getTemperature(res.getOptimalTime());
			double hi = weatherData.getHeatIndex(res.getOptimalTime())
					.orElse(temp);

			double costRouteTemp = helper.routeWeight(res.getOptimalPath(),
					res.getOptimalTime(), WeightingType.TEMPERATURE);
			double costRouteHI = helper.routeWeight(res.getOptimalPath(),
					res.getOptimalTime(), WeightingType.HEAT_INDEX);

			OptimalTimeResultItem resultItem = new OptimalTimeResultItem(
					res.getPlace(), res.getOptimalTime(), resultType,
					res.getRank().orElse(-1), res.getOptimalValue(),
					res.getDistance(), res.getDuration(), temp, hi,
					costRouteTemp, costRouteHI, res.getOptimalPath().calcPoints());
			resultItems.add(resultItem);
		}
		return resultItems;
	}

	private List<OptimalTimeResultItem> getReferenceResultItem(
			OptimalTimeEvaluationItem item) {

		Predicate<Entity> nodeFilter = EntityFilter
				.containsAnyTag(item.getTargetTags())
				.and(EntityFilter::hasOpeningHours);

		GHPoint start = OSMUtils.getGHPoint(item.start);

		int maxResults = item.maxResults != null ? item.maxResults
				: this.nearbySearchMaxResults;
		double maxDist = item.maxDistance != null ? item.maxDistance
				: this.maxDistance;

		List<Node> places = osmData.kNearestNeighbor(start, maxResults, maxDist,
				nodeFilter);

		RoutingHelper helper = new RoutingHelper(hopper);

		List<OptimalTimeResultItem> resultItems = new ArrayList<>(
				places.size());

		for (Node place : places) {
			GHPoint placePoint = OSMUtils.getGHPoint(place);

			Optional<Path> path = helper.routePathShortest(start, placePoint)
					.get();
			if (!path.isPresent())
				continue;

			Optional<OSMOpeningHours> openingHours = osmData
					.getOpeningHours(place.getId());

			if (!openingHours.isPresent())
				continue;

			Duration tWalk = Duration.ofMillis(path.get().getTime());
			ZonedDateTime tNow = item.now.atZone(ZoneId.systemDefault());
			ZonedDateTime tUpper = tNow.plus(tWalk).plus(item.timeBuffer);

			double temp = weatherData.getTemperature(tNow.toLocalDateTime());
			double rh = weatherData.getRelativeHumidity(tNow.toLocalDateTime());

			double hi;
			if (HeatIndex.isValidTemperature(temp)
					&& HeatIndex.isValidHumidity(rh))
				hi = HeatIndex.heatIndex(temp, rh);
			else
				hi = temp;

			if (openingHours.get().isOpenedForTime(tNow)
					&& openingHours.get().isOpenedForTime(tUpper)) {

				double costRouteTemp = helper.routeWeight(path.get(), item.now,
						WeightingType.TEMPERATURE);
				double costRouteHI = helper.routeWeight(path.get(), item.now,
						WeightingType.HEAT_INDEX);

				resultItems.add(new OptimalTimeResultItem(place, item.now,
						OptimalTimeResultType.REFERENCE, -1, hi,
						path.get().getDistance(), path.get().getTime(),
						weatherData.getTemperature(item.now),
						weatherData.getHeatIndex(item.now)
								.orElse(weatherData.getTemperature(item.now)),
						costRouteTemp, costRouteHI, path.get().calcPoints()));
			}
		}

		return Seq
				.seq(resultItems).sorted((i1, i2) -> Double
						.compare(i1.getDistance(), i2.getDistance()))
				.zipWithIndex().map(t -> {
					int rank = ((int) t.v2().longValue()) + 1;
					return t.v1().setRank(rank);
				}).toList();

	}

	private static void writeResults(List<OptimalTimeResultRecord> results,
			File outFile) throws IOException {

		logger.info("write " + results.size() + " result(s) to "
				+ outFile.getAbsolutePath() + ", exists = " + outFile.exists());

		FileWriter writer = new FileWriter(outFile, false);

		String header = String.join(Evaluator.DELIMITER, "id", "start.id",
				"start.lat", "start.lon", "time.now", "tags", "time.buffer",
				"time.earliest", "time.latest", "max.distance", "max.results",
				"place.id", "place.lat", "place.lon", "time.opt", "type",
				"rank", "value.opt", "distance", "duration", "temperature",
				"heatindex", "cost.route.temperature", "cost.route.heatindex",
				"points");

		writer.append(header);

		int id = 0;
		for (OptimalTimeResultRecord rec : results) {
			String evalItem = rec.getItem().toCsvRecord();
			for (OptimalTimeResultItem item : rec.getResults()) {
				writer.append("\n");
				writer.append(String.valueOf(id));
				writer.append(Evaluator.DELIMITER);
				writer.append(evalItem);
				writer.append(Evaluator.DELIMITER);
				writer.append(item.toCsvRecord());
			}
			id++;
		}

		writer.flush();
		writer.close();
	}

	public void randomEvaluationItems(int n) {
		this.evaluationItems = new OptimalTimeEvaluationItemsFactory(osmData,
				weatherData, weatherData.getTimeRange()).setNoStarts(n)
						.createEvaluationItems();
	}

	public OptimalTimeEvaluationItemsFactory createEvaluationItemsFactory(
			TimeRange<LocalDateTime> timeRange) {
		return new OptimalTimeEvaluationItemsFactory(osmData, weatherData,
				timeRange);
	}

	public void setShopTags(List<String> tags, boolean append) {
		setTags(SHOP, tags, append);
	}

	public void setAmenityTags(List<String> tags, boolean append) {
		setTags(AMENITY, tags, append);
	}

	public void setTags(String key, Collection<String> values, boolean append) {
		List<Tag> tagList = createTags(key, values);
		if (append)
			this.targetTags.addAll(tagList);
		else
			this.targetTags = tagList;
	}

	static List<Tag> createTags(String key, Collection<String> values) {
		return values.stream().map(t -> new Tag(key, t))
				.collect(Collectors.toList());
	}

	public HeatStressGraphHopper getHopper() {
		return hopper;
	}

	public void setHopper(HeatStressGraphHopper hopper) {
		this.hopper = hopper;
	}

	public List<OptimalTimeEvaluationItem> getEvaluationItems() {
		return evaluationItems;
	}

	public void setEvaluationItems(OptimalTimeEvaluationItemsFactory factory) {
		this.evaluationItems = factory.createEvaluationItems();
	}

	public void setEvaluationItems(
			List<OptimalTimeEvaluationItem> evaluationItems) {
		this.evaluationItems = evaluationItems;
	}

	public List<Tag> getTargetTags() {
		return targetTags;
	}

	public void setTargetTags(List<Tag> targetTags) {
		this.targetTags = targetTags;
	}

	public Double getMaxDistance() {
		return maxDistance;
	}

	public void setMaxDistance(Double maxDistance) {
		this.maxDistance = maxDistance;
	}

	public int getNearbySearchMaxResults() {
		return nearbySearchMaxResults;
	}

	public void setNearbySearchMaxResults(int nearbySearchMaxResults) {
		this.nearbySearchMaxResults = nearbySearchMaxResults;
	}

	public List<OptimalTimeResultRecord> getResults() {
		return results;
	}

	public void setResults(List<OptimalTimeResultRecord> results) {
		this.results = results;
	}

	public OptionalLong getExecutionTime() {
		return this.executionTime != null ? OptionalLong.of(executionTime)
				: OptionalLong.empty();
	}

	public OptimalTimeFinder getOptimalTimeFinder() {
		return optimalTimeFinder;
	}

	public void setOptimalTimeFinder(OptimalTimeFinder optimalTimeFinder) {
		this.optimalTimeFinder = optimalTimeFinder;
	}
}
