package joachimrussig.heatstressrouting.webapi;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.openstreetmap.osmosis.core.domain.v0_6.Bound;

import joachimrussig.heatstressrouting.osmdata.EntityFilter;
import joachimrussig.heatstressrouting.routing.RoutingHelper;
import joachimrussig.heatstressrouting.util.TimeRange;
import joachimrussig.heatstressrouting.webapi.util.JsonUtils;

@Path("/v1/info")
public class ServerInfo {

	@Context
	ServletContext context;
	@Inject
	RoutingHelper routingHelper;

	// https://github.com/Codingpedia/demo-rest-jersey-spring/blob/master/src/main/java/org/codingpedia/demo/rest/resource/manifest/ManifestService.java
	private Attributes getManifestAttributes()
			throws IOException {
		InputStream resourceAsStream = context
				.getResourceAsStream("/META-INF/MANIFEST.MF");
		Manifest mf = new Manifest();
		mf.read(resourceAsStream);
		Attributes atts = mf.getMainAttributes();

		return atts;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response getInfo() {
		Attributes attrs = null;
		try {
			attrs = getManifestAttributes();
		} catch (IOException e) {
		}

		JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
		jsonBuilder.add("service", "heat stress routing");

		if (attrs != null) {

			jsonBuilder.add("version",
					String.valueOf(attrs.getValue("Implementation-Version")));

			jsonBuilder.add("build_time",
					String.valueOf(attrs.getValue("Build-Time")));
		}

		if (routingHelper != null) {
			Optional<Bound> boundingBox = routingHelper.getHopper().getOsmData()
					.getBoundingBox();
			if (boundingBox.isPresent()) {
				JsonArray coords = Json.createArrayBuilder()
						.add(boundingBox.get().getBottom())
						.add(boundingBox.get().getLeft())
						.add(boundingBox.get().getTop())
						.add(boundingBox.get().getRight()).build();
				jsonBuilder.add("bbox", coords);
			}

			TimeRange<LocalDateTime> timeRange = routingHelper.getHopper()
					.getWeatherData().getTimeRange();
			jsonBuilder.add("time_range",
					Json.createObjectBuilder()
							.add("from", timeRange.getFrom().toString())
							.add("to", timeRange.getTo().toString()));
		}

		jsonBuilder.add("place_types", JsonUtils
				.toJsonArrayString(EntityFilter.TAG_MAP.keySet().iterator()));

		JsonObject json = jsonBuilder.build();
		return Response.ok(JsonUtils.toString(json)).build();
	}

}
