package joachimrussig.heatstressrouting;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.github.davidmoten.guavamini.Lists;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.shapes.GHPoint;

import joachimrussig.heatstressrouting.evaluation.optimaltime.OptimalTimeEvaluationItem;
import joachimrussig.heatstressrouting.evaluation.optimaltime.OptimalTimeEvaluationItemsFactory;
import joachimrussig.heatstressrouting.evaluation.optimaltime.OptimalTimeEvaluator;
import joachimrussig.heatstressrouting.evaluation.routing.RoutingEvaluator;
import joachimrussig.heatstressrouting.routing.RoutingHelper;
import joachimrussig.heatstressrouting.routing.RoutingRequest;
import joachimrussig.heatstressrouting.routing.RoutingRequestBuilder;
import joachimrussig.heatstressrouting.routing.RoutingResponse;
import joachimrussig.heatstressrouting.routing.weighting.WeightingType;
import joachimrussig.heatstressrouting.util.TimeRange;
import joachimrussig.heatstressrouting.util.Utils;

/**
 * Utility class that contains some Tests and evaluation configurations.
 * 
 * @author Joachim Russig
 *
 */
public class Tests {

	static void runRoutingEvaluatorSetting1() {
		// Setting 1:
		// - 1000 random starts and destinations
		// - 10 Random dates
		// - evaluated every 4 hours in range 07:00 to 23:00
		RoutingEvaluator re = new RoutingEvaluator();
		try {
			re.setOsmFile(new File(HeatStressRouting.OSM_FILE));
			re.setWeatherDataFile(new File(HeatStressRouting.WEATHER_DATA));
			re.setWaySegmentsFile(
					new File(HeatStressRouting.WAY_SEGMENT_WEIGHTS));
			re.setWeights(0.25, 0.75);
			re.init();
			re.setSeed(2375737176707274901L);
			re.randomStartsAndDestinations(1000);
			re.randomDates(10, LocalDate.of(2015, 6, 1),
					LocalDate.of(2015, 8, 31));
			re.sequentialTimePoints(LocalTime.of(7, 0), LocalTime.of(23, 0),
					240);
			re.exportResultsTo(new File(HeatStressRouting.OUT_DIR
					+ "routes_weighted_setting1_1000_10_240min.csv"));
			re.setRoutingAlgo(Parameters.Algorithms.DIJKSTRA_BI);
			re.run();
			System.out.println("results size: " + re.getResultRecords().size());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static void runRoutingEvaluatorSetting2() {
		// Setting 2:
		// - 100 random starts and destinations
		// - 10 Random dates
		// - evaluated every 30 minutes in range 00:00 to 23:30
		RoutingEvaluator re = new RoutingEvaluator();
		try {
			re.setOsmFile(new File(HeatStressRouting.OSM_FILE));
			re.setWeatherDataFile(new File(HeatStressRouting.WEATHER_DATA));
			re.setWaySegmentsFile(
					new File(HeatStressRouting.WAY_SEGMENT_WEIGHTS));
			re.setWeights(0.25, 0.75);
			re.init();
			re.setSeed(2375737176707274901L);
			re.randomStartsAndDestinations(100);
			re.randomDates(10, LocalDate.of(2015, 6, 1),
					LocalDate.of(2015, 8, 31));
			re.sequentialTimePoints(LocalTime.MIN, LocalTime.MAX, 30);
			re.exportResultsTo(new File(HeatStressRouting.OUT_DIR
					+ "routes_weighted_setting2_100_10_30min.csv"));
			re.setRoutingAlgo(Parameters.Algorithms.DIJKSTRA_BI);
			re.run();
			System.out.println("results size: " + re.getResultRecords().size());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static void runOptimalTimeEvaluation() {
		OptimalTimeEvaluator evaluator = new OptimalTimeEvaluator();
		evaluator.setOsmFile(new File(HeatStressRouting.OSM_FILE));
		evaluator.setWeatherDataFile(new File(HeatStressRouting.WEATHER_DATA));
		evaluator.setWaySegmentsFile(
				new File(HeatStressRouting.WAY_SEGMENT_WEIGHTS));
		evaluator.setSeed(2375737176707274901L);
		try {
			evaluator.init();
			TimeRange<LocalDateTime> timeRange = new TimeRange<>(
					LocalDate.of(2015, 6, 1).atTime(LocalTime.MIN),
					LocalDate.of(2015, 8, 31).atTime(LocalTime.MAX));
			List<Tag> tags = Lists.newArrayList(new Tag("shop", "supermarket"),
					new Tag("shop", "bakery"), new Tag("shop", "chemist"),
					new Tag("amenity", "pharmacy"));
			OptimalTimeEvaluationItemsFactory factory = evaluator
					.createEvaluationItemsFactory(timeRange);

			List<OptimalTimeEvaluationItem> items = factory.setSeed(42L)
					.setNoStarts(750).setTags(tags).setNoTags(1)
					.setMaxDistance(1000.0).setMaxResults(5)
					.setMinTemperature(20.0)
					.setNowTimeRange(new TimeRange<>(LocalTime.of(8, 0),
							LocalTime.of(20, 0)))
					.createEvaluationItems();
			evaluator.exportResultsTo(new File(
					HeatStressRouting.OUT_DIR + "optimaletime_750_8-20.csv"));
			evaluator.setMaxDistance(2000.0);
			evaluator.setEvaluationItems(items);
			evaluator.run();

			evaluator.getResults().forEach(System.out::println);

			String timeStr;
			if (evaluator.getExecutionTime().isPresent())
				timeStr = Utils.formatDurationMills(
						evaluator.getExecutionTime().getAsLong());
			else
				timeStr = "EROOR";
			System.out.println("Evaluated " + evaluator.getResults().size()
					+ " item(s) in " + timeStr);
			if (evaluator.getOutFile().isPresent()
					&& evaluator.isExportResults()) {
				System.out.println("Exported Resutls to "
						+ evaluator.getOutFile().get().getAbsolutePath());
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	static void testRouteWeightCalculation() {
		GHPoint start = new GHPoint(49.0118083, 8.4251357);
		GHPoint dest = new GHPoint(49.0126868, 8.4065707);
		LocalDateTime time = LocalDateTime.of(2015, 8, 31, 10, 0);

		try {

			String dataDir = HeatStressRouting.BASE_DIR;

			File osmFile = Paths.get(dataDir, HeatStressRouting.OSM_FILE_NAME)
					.toFile();
			File weatherDataFile = Paths
					.get(dataDir, HeatStressRouting.WEATHER_FILE_NAME).toFile();
			File waySegmentsFile = Paths
					.get(dataDir, HeatStressRouting.WAY_SEGMENTS_FILE_NAME)
					.toFile();

			RoutingHelper helper = new RoutingHelper(RoutingHelper
					.createHopper(osmFile, weatherDataFile, waySegmentsFile));
			List<RoutingRequest> requests = Arrays
					.stream(WeightingType.values())
					.map(w -> new RoutingRequestBuilder(start, dest, w, time)
							.build())
					.collect(Collectors.toList());
			requests.addAll(
					Arrays.stream(WeightingType.values())
							.map(w -> new RoutingRequestBuilder(dest, start, w,
									time).build())
							.collect(Collectors.toList()));
			List<RoutingResponse> responses = requests.stream()
					.map(req -> helper.route(req)).collect(Collectors.toList());

			for (RoutingResponse rsp : responses) {
				System.out.println(
						"weighting: " + rsp.getRequest().getWeightingType());
				System.out
						.println("\tdistance: " + rsp.getBest().getDistance());
				System.out.println("\ttime: " + rsp.getBest().getTime());
				System.out
						.println("\tweight: " + rsp.getBest().getRouteWeight());
				System.out.println("\tresponse: " + rsp.toString());
				System.out.println(
						"\tweights: " + Arrays.stream(WeightingType.values())
								.map(w -> w.toString() + " = "
										+ String.valueOf(helper.routeWeight(
												rsp.getPaths().get(0),
												rsp.getRequest().getTime(), w)))
								.collect(Collectors.joining(", ")));

			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static void testRoutingEvaluator() {

		RoutingEvaluator re = new RoutingEvaluator();
		try {
			re.setOsmFile(new File(HeatStressRouting.OSM_FILE));
			re.setWeatherDataFile(new File(HeatStressRouting.WEATHER_DATA));
			re.setWaySegmentsFile(
					new File(HeatStressRouting.WAY_SEGMENT_WEIGHTS));
			re.init();
			re.setSeed(2375737176707274901L);
			re.randomStartsAndDestinations(10);
			re.randomDates(2, LocalDate.of(2015, 6, 1),
					LocalDate.of(2015, 8, 31));
			re.sequentialTimePoints(LocalTime.of(7, 0), LocalTime.of(23, 0),
					240);
			re.setRoutingAlgo(Parameters.Algorithms.DIJKSTRA_BI);
			re.run();
			System.out.println("results size: " + re.getResultRecords().size());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static void testOptimalTimeEvaluation() {
		OptimalTimeEvaluator evaluator = new OptimalTimeEvaluator();
		evaluator.setOsmFile(new File(HeatStressRouting.OSM_FILE));
		evaluator.setWeatherDataFile(new File(HeatStressRouting.WEATHER_DATA));
		evaluator.setWaySegmentsFile(
				new File(HeatStressRouting.WAY_SEGMENT_WEIGHTS));
		evaluator.setSeed(2375737176707274901L);
		try {
			evaluator.init();
			TimeRange<LocalDateTime> timeRange = new TimeRange<>(
					LocalDate.of(2015, 6, 1).atTime(LocalTime.MIN),
					LocalDate.of(2015, 8, 31).atTime(LocalTime.MAX));
			List<Tag> tags = Lists.newArrayList(new Tag("shop", "supermarket"),
					new Tag("shop", "bakery"), new Tag("shop", "chemist"),
					new Tag("amenity", "pharmacy"));
			OptimalTimeEvaluationItemsFactory factory = evaluator
					.createEvaluationItemsFactory(timeRange);

			List<OptimalTimeEvaluationItem> items = factory.setSeed(42L)
					.setNoStarts(50).setTags(tags).setNoTags(1)
					.setMaxDistance(1000.0).setMaxResults(5)
					.setMinTemperature(20.0)
					.setNowTimeRange(new TimeRange<>(LocalTime.of(8, 0),
							LocalTime.of(20, 0)))
					.createEvaluationItems();
			// evaluator.exportResultsTo(
			// new File(OUT_DIR + "optimaletime_750_8-20.csv"));
			evaluator.setMaxDistance(2000.0);
			evaluator.setEvaluationItems(items);
			evaluator.run();

			evaluator.getResults().forEach(System.out::println);

			String timeStr;
			if (evaluator.getExecutionTime().isPresent())
				timeStr = Utils.formatDurationMills(
						evaluator.getExecutionTime().getAsLong());
			else
				timeStr = "EROOR";
			System.out.println("Evaluated " + evaluator.getResults().size()
					+ " item(s) in " + timeStr);
			if (evaluator.getOutFile().isPresent()
					&& evaluator.isExportResults()) {
				System.out.println("Exported Resutls to "
						+ evaluator.getOutFile().get().getAbsolutePath());
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
