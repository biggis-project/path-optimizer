package joachimrussig.heatstressrouting.webapi;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.tuple.Pair;
import org.openstreetmap.osmosis.core.domain.v0_6.Bound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.graphhopper.util.shapes.GHPoint;

import joachimrussig.heatstressrouting.osmdata.OSMUtils;
import joachimrussig.heatstressrouting.routing.RoutingHelper;
import joachimrussig.heatstressrouting.routing.RoutingRequest;
import joachimrussig.heatstressrouting.routing.RoutingRequestBuilder;
import joachimrussig.heatstressrouting.routing.RoutingResponse;
import joachimrussig.heatstressrouting.routing.weighting.WeightingType;
import joachimrussig.heatstressrouting.util.Result;
import joachimrussig.heatstressrouting.util.TimeRange;
import joachimrussig.heatstressrouting.webapi.util.JsonResponseBuilder;
import joachimrussig.heatstressrouting.webapi.util.JsonUtils;
import joachimrussig.heatstressrouting.webapi.util.ResponseStatus;
import joachimrussig.heatstressrouting.webapi.util.WebApiUtils;

@Path("/v1/routing")
public class Routing {

	private Logger logger = LoggerFactory.getLogger(Routing.class);

	@Inject
	RoutingHelper routingHelper;

	// Example request
	// http://localhost:8080/heatstressrouting/api/v1/routing?start=49.0118083,8.4251357&destination=49.0126868,8.4065707&time=2015-08-31T10:00:00

	@GET
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response getRoute(@Context HttpServletRequest request,
			@QueryParam("start") String start,
			@QueryParam("destination") String destination,
			@QueryParam("time") String time,
			@DefaultValue("heatindex") @QueryParam("weighting") String weighting) {

		// TODO code clean up
		// TODO improve error handling (collect bad request errors and return
		// them as a single response)

		logger.info("requested url: " + request.getRequestURI().toString()
				+ request.getQueryString());

		time = time.trim();

		// Collect BadRequests to create a single response
		List<String> badRequestMessages = new ArrayList<>();

		final GHPoint from = WebApiUtils.parseGHPoint(start)
				.unwrapOrElse(err -> {
					badRequestMessages.add("start (" + start
							+ ") could not be parsed: " + err.getMessage()
							+ "; 'start' must be a pair of latitude and longitude seperated by a comma (','), e.g. '49.0118083,8.4251357')");

					return null;
				});

		final GHPoint to = WebApiUtils.parseGHPoint(destination)
				.unwrapOrElse(err -> {
					badRequestMessages.add("destination (" + destination
							+ ") could not be parsed: " + err.getMessage()
							+ "; 'start' must be a pair of latitude and longitude seperated by a comma (','), e.g. '49.0118083,8.4251357')");

					return null;
				});

		Optional<Bound> bbox = routingHelper.getHopper().getOsmData()
				.getBoundingBox();
		if (bbox.isPresent() && from != null && to != null) {
			if (!OSMUtils.withinBoundingBox(from, bbox.get())) {
				badRequestMessages.add("start (" + from.getLat() + ","
						+ from.getLon()
						+ ") is not within the bounding box (bbox = "
						+ bbox.toString() + "). "
						+ "Use 'heatstressrouting/api/v1/info' to recive the supported bounding box.");
			}
			if (!OSMUtils.withinBoundingBox(to, bbox.get())) {
				badRequestMessages.add("destination (" + to.getLat() + ","
						+ to.getLon()
						+ ") is not within the bounding box (bbox = "
						+ bbox.toString() + "). "
						+ "Use 'heatstressrouting/api/v1/info' to recive the supported bounding box.");
			}

		}

		final LocalDateTime localDateTime = WebApiUtils.parseLocalDateTime(time)
				.unwrapOrElse(err -> {
					badRequestMessages.add(err.getMessage()
							+ ". The data time must be either the string 'now' or "
							+ "in the form '2015-08-31T10:00:00'");

					return null;
				});

		TimeRange<LocalDateTime> timeRange = routingHelper.getTimeRange();
		if (localDateTime != null
				&& !timeRange.containsInclusive(localDateTime)) {
			badRequestMessages.add("time '" + time.toString()
					+ "' is not with in the supproted time range ("
					+ timeRange.toString() + ")."
					+ " Use 'heatstressrouting/api/v1/info' to recive the supported time range.");
		}


		Result<Set<WeightingType>, List<String>> weightingTypes = WebApiUtils
				.parseWeightingTypes(weighting, WeightingType.SHORTEST);

		if (weightingTypes.isError()) {
			badRequestMessages.add("unknown weighting(s) "
					+ weightingTypes.unwrapError().stream()
							.collect(Collectors.joining(", "))
					+ "; weighting must be a comma seperated list of the following values: "
					+ Arrays.stream(WeightingType.values())
							.map(WeightingType::toString)
							.collect(Collectors.joining(", ")));
		}

		if (!badRequestMessages.isEmpty()) {
			// The Request contains errors, so we return it
			return new JsonResponseBuilder(ResponseStatus.BAD_REQUEST)
					.addStringMessages(badRequestMessages).build();
		}

		// logger.info("weightingTypes = "
		// + weightingTypes.unwrap().stream().map(WeightingType::toString)
		// .collect(Collectors.joining(", ")));

		List<RoutingRequest> routingRequests = weightingTypes.unwrap().stream()
				.map(w -> new RoutingRequestBuilder(from, to, w, localDateTime)
						.build())
				.collect(Collectors.toList());

		List<RoutingResponse> routingResponses = routingRequests.stream()
				.map(req -> this.routingHelper.route(req))
				.collect(Collectors.toList());

		if (routingResponses.stream().anyMatch(RoutingResponse::hasErrors)) {
			List<String> errors = routingResponses.stream()
					.flatMap(rsp -> rsp.getErrors().stream())
					.map(Throwable::getMessage).collect(Collectors.toList());

			logger.error("INTERNAL_ERROR: "
					+ errors.stream().collect(Collectors.joining(", ")));

			return new JsonResponseBuilder(ResponseStatus.INTERNAL_ERROR)
					.addStringMessages(errors).build();
		} else {

			JsonResponseBuilder builder = new JsonResponseBuilder(
					ResponseStatus.OK);
			for (RoutingResponse rsp : routingResponses) {
				// FIXME the weight returned by rsp.getBest().getRouteWeight()
				// differs from the result returned by helper.routeWeight()
				Map<String, Double> weights = weightingTypes.unwrap().stream()
						.map(w -> Pair.of(w.toString(),
								routingHelper.routeWeight(rsp.getPaths().get(0),
										rsp.getRequest().getTime(), w)))
						.collect(
								Collectors.toMap(Pair::getKey, Pair::getValue));
				logger.debug("rsp: " + rsp);
				logger.debug("weights: " + weights.entrySet().stream()
						.map(e -> e.getKey() + "=" + e.getValue())
						.collect(Collectors.joining(",")));

				JsonObject rspJson = JsonUtils.toJsonObject(rsp, weights);
				builder.addResult(
						rsp.getRequest().getWeightingType().toString(),
						rspJson);
			}
			return builder.build();
		}
	}

}
