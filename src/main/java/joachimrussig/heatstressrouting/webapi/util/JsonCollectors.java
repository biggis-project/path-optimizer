package joachimrussig.heatstressrouting.webapi.util;

import java.util.Map;
import java.util.stream.Collector;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 * Based on:
 * http://www.adam-bien.com/roller/abien/entry/converting_a_map_string_string, retrived on 2016-07-27
 * 
 * @author joachimrussig
 *
 */
public interface JsonCollectors {

	static <T> Collector<Map.Entry<T, T>, ?, JsonObjectBuilder> toJsonBuilderMap() {
		return Collector.of(Json::createObjectBuilder, (t, u) -> {
			t.add(String.valueOf(String.valueOf(u.getKey())),
					String.valueOf(u.getValue()));
		}, JsonCollectors::merge);
	}

	static <T> Collector<JsonObject, ?, JsonArrayBuilder> toJsonArrayBuilder() {
		return Collector.of(Json::createArrayBuilder, (t, v) -> t.add(v),
				JsonCollectors::mergeArrayBuilder);
	}

	static JsonObjectBuilder merge(JsonObjectBuilder left,
			JsonObjectBuilder right) {
		JsonObjectBuilder retVal = Json.createObjectBuilder();
		JsonObject leftObject = left.build();
		JsonObject rightObject = right.build();
		leftObject.keySet().stream()
				.forEach((key) -> retVal.add(key, leftObject.get(key)));
		rightObject.keySet().stream()
				.forEach((key) -> retVal.add(key, rightObject.get(key)));
		return retVal;
	}

	static JsonArrayBuilder mergeArrayBuilder(JsonArrayBuilder left, JsonArrayBuilder right) {
    	JsonArrayBuilder ret = Json.createArrayBuilder();
    	JsonArray leftArray = left.build();
    	JsonArray rightArray = right.build();
    	leftArray.stream().forEach(v -> ret.add(v));
    	rightArray.stream().forEach(v -> ret.add(v));
    	return ret;
    }

}
