package joachimrussig.heatstressrouting.webapi.util;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint;

import joachimrussig.heatstressrouting.optimaltime.nearbysearch.NearbySearchResult;
import joachimrussig.heatstressrouting.osmdata.OSMUtils;
import joachimrussig.heatstressrouting.routing.RoutingResponse;

public class JsonUtils {

	private static final Logger logger = LoggerFactory
			.getLogger(JsonUtils.class);

	private static JsonWriterFactory FACTORY_INSTANCE;

	// http://stackoverflow.com/questions/4105795/pretty-print-json-in-java/32480523#32480523
	public static String toString(final JsonStructure json) {

		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		final JsonWriter jsonWriter = getPrettyJsonWriterFactory()
				.createWriter(outputStream, Charset.forName("UTF-8"));

		jsonWriter.write(json);
		jsonWriter.close();

		String jsonString;
		try {
			jsonString = new String(outputStream.toByteArray(), "UTF8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

		return jsonString;
	}

	private static JsonWriterFactory getPrettyJsonWriterFactory() {
		if (null == FACTORY_INSTANCE) {
			final Map<String, Object> properties = new HashMap<>(1);
			properties.put(JsonGenerator.PRETTY_PRINTING, true);
			FACTORY_INSTANCE = Json.createWriterFactory(properties);
		}
		return FACTORY_INSTANCE;
	}

	public static <T> JsonArray toJsonArrayString(Iterator<String> items) {
		JsonArrayBuilder builder = Json.createArrayBuilder();
		while (items.hasNext()) {
			builder.add(items.next());
		}
		return builder.build();
	}

	public static JsonArray toJsonArray(Iterator<JsonObject> jsonObjects) {
		JsonArrayBuilder builder = Json.createArrayBuilder();
		while (jsonObjects.hasNext()) {
			builder.add(jsonObjects.next());
		}
		return builder.build();
	}

	public static JsonObject toJsonObject(RoutingResponse rsp,
			Map<String, Double> routeWeights) {
		JsonObjectBuilder builder = Json.createObjectBuilder();

		builder.add("weighting", rsp.getRequest().getWeightingType().toString())
				.add("start", toJsonArray(rsp.getRequest().getStart()))
				.add("destination",
						toJsonArray(rsp.getRequest().getDestination()))
				.add("distance", rsp.getBest().getDistance())
				.add("duration", rsp.getBest().getTime())
//				.add("route_weight", rsp.getBest().getRouteWeight())
				.add("route_weights", toJsonObject(routeWeights))
				.add("path", toJsonArray((rsp.getBest().getPoints())));

		return builder.build();

	}
	
	public static JsonObject toJsonObject(Map<String, Double> vals) {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		for (Map.Entry<String, Double> val : vals.entrySet()) {
			builder.add(val.getKey(), val.getValue());
		}
		return builder.build();
 	}
	
	public static JsonObject jsonObject(String key, JsonValue val) {
		return Json.createObjectBuilder().add(key, val).build();
	}

	public static JsonObject toJsonObject(
			NearbySearchResult nearbySearchResult) {
		JsonObjectBuilder builder = Json.createObjectBuilder();

		// TODO find a better method
		JsonValue name = nearbySearchResult.getPlace().getTags().stream()
				.filter(t -> t.getKey().equalsIgnoreCase("name"))
				.map(Tag::getValue)
				.map(n -> (JsonValue) Json.createObjectBuilder().add("name", n)
						.build().getJsonString("name"))
				.findAny().orElse(JsonValue.NULL);

		logger.debug("name = " + name.toString());

		builder.add("rank", nearbySearchResult.getRank().getAsInt())
				.add("name", name)
				.add("osm_id", nearbySearchResult.getPlace().getId())
				.add("location",
						toJsonArray(OSMUtils
								.getGHPoint(nearbySearchResult.getPlace())))
				.add("opening_hours",
						nearbySearchResult.getPlace().getTags().stream()
								.filter(t -> t.getKey()
										.equalsIgnoreCase("opening_hours"))
								.findAny().get().getValue())
				.add("optimal_time",
						nearbySearchResult.getOptimalTime().toString())
				.add("optimal_value", nearbySearchResult.getOptimalValue())
				.add("distance", nearbySearchResult.getDistance())
				.add("duration", nearbySearchResult.getDuration())
				.add("path_optimal",
						toJsonArray(nearbySearchResult.getOptimalPath().calcPoints()))
				.add("distance_shortest", nearbySearchResult.getShortestPath().getDistance())
				.add("duration_shortest", nearbySearchResult.getShortestPath().getTime())
				.add("path_shortest",
						toJsonArray(nearbySearchResult.getShortestPath().calcPoints()));

		return builder.build();
	}

	/**
	 * Serializes the latitude and longitude value (in that order) of the
	 * {@link GHPoint} instance {@code ghPoint} as an {@link JsonArray}, e.g.
	 * [49.0118083, 8.4251357].
	 * 
	 * @param ghPoint
	 *            the point to serialize
	 * @return the serialized point
	 */
	public static JsonArray toJsonArray(GHPoint ghPoint) {
		return Json.createArrayBuilder().add(ghPoint.getLat())
				.add(ghPoint.getLon()).build();
	}

	/**
	 * Serializes the {@link PointList} as JsonArray of Points, e.g.
	 * [[[49.0118083, 8.4251357], [49.0126868, 8.4065707]].
	 * 
	 * @param points
	 * @return
	 */
	public static JsonArray toJsonArray(PointList points) {
		JsonArrayBuilder builder = Json.createArrayBuilder();
		for (GHPoint point : points) {
			builder.add(toJsonArray(point));
		}
		return builder.build();
	}

}
