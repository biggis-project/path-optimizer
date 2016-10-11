package joachimrussig.heatstressrouting.webapi;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
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

import org.apache.commons.lang3.time.StopWatch;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.graphhopper.util.shapes.GHPoint;

import joachimrussig.heatstressrouting.optimaltime.nearbysearch.NearbySearchHelper;
import joachimrussig.heatstressrouting.optimaltime.nearbysearch.NearbySearchRequest;
import joachimrussig.heatstressrouting.optimaltime.nearbysearch.NearbySearchResponse;
import joachimrussig.heatstressrouting.osmdata.EntityFilter;
import joachimrussig.heatstressrouting.util.TimeRange;
import joachimrussig.heatstressrouting.webapi.util.JsonCollectors;
import joachimrussig.heatstressrouting.webapi.util.JsonResponseBuilder;
import joachimrussig.heatstressrouting.webapi.util.JsonUtils;
import joachimrussig.heatstressrouting.webapi.util.ResponseStatus;
import joachimrussig.heatstressrouting.webapi.util.WebApiUtils;

@Path("/v1/optimaltime")
public class OptimalTime {

	private Logger logger = LoggerFactory.getLogger(OptimalTime.class);

	@Inject
	NearbySearchHelper nearbySearchHelper;

	// Example request
	// http://localhost:8080/heatstressrouting/api/v1/optimaltime?start=49.0118083,8.4251357&time=2015-08-31T10:00:00&place_type=supermarket

	@GET
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response getOptimalTime(@Context HttpServletRequest request,
			@QueryParam("start") String start, @QueryParam("time") String time,
			@QueryParam("place_type") String placeType,
			@DefaultValue("5") @QueryParam("max_results") int maxResults,
			@DefaultValue("1000.0") @QueryParam("max_distance") double maxDistance,
			@DefaultValue("15") @QueryParam("time_buffer") int timeBufferInt,
			@DefaultValue("null") @QueryParam("earliest_time") String earliestTimeStr,
			@DefaultValue("null") @QueryParam("latest_time") String latestTimeStr) {

		logger.info("requested url: " + request.getRequestURI().toString()
				+ request.getQueryString());

		List<String> badRequestMessages = new ArrayList<>();

		final GHPoint startPoint = WebApiUtils.parseGHPoint(start)
				.unwrapOrElse(err -> {
					badRequestMessages.add("start (" + start
							+ ") could not be parsed: " + err.getMessage()
							+ "; 'start' must be a pair of latitude and longitude seperated by a comma (','), e.g. '49.0118083,8.4251357')");

					return null;
				});

		final LocalDateTime now = WebApiUtils.parseLocalDateTime(time)
				.unwrapOrElse(err -> {
					badRequestMessages.add(err.getMessage()
							+ ". The data time must be either the string 'now' or "
							+ "in the form '2015-08-31T10:00:00'");

					return null;
				});

		final LocalDateTime earliestTime = WebApiUtils
				.parseLocalDateTimeNullable(earliestTimeStr)
				.unwrapOrElse(err -> {
					badRequestMessages.add(err.getMessage()
							+ ". The data time must be either the string 'now', 'null' or "
							+ "in the form '2015-08-31T10:00:00'");

					return null;
				});

		final LocalDateTime latestTime = WebApiUtils
				.parseLocalDateTimeNullable(latestTimeStr).unwrapOrElse(err -> {
					badRequestMessages.add(err.getMessage()
							+ ". The data time must be either the string 'now', 'null' or "
							+ "in the form '2015-08-31T10:00:00'");

					return null;
				});

		if (earliestTime != null && latestTime != null
				&& !earliestTime.isBefore(latestTime)) {
			badRequestMessages
					.add("if 'earliest_time' and 'latest_time' are specified, "
							+ "then 'earliest_time' must be before 'latest_time'");
		}

		final Duration timeBuffer;
		if (timeBufferInt >= 0) {
			timeBuffer = Duration.ofMinutes(timeBufferInt);
		} else {
			timeBuffer = Duration.ofMinutes(15);
			badRequestMessages.add("'time_buffer' must be non negative");
		}

		TimeRange<LocalDateTime> timeRange = nearbySearchHelper
				.getRoutingHelper().getTimeRange();
		if (now != null && !timeRange.containsInclusive(now)) {
			badRequestMessages.add("time '" + now.toString()
					+ "' is not with in the supproted time range ("
					+ timeRange.toString()
					+ "). Use 'heatstressrouting/api/v1/info' to recive the supported time range.");
		}

		List<String> tagList = WebApiUtils.parseTagsList(placeType);
		if (!tagList.stream().allMatch(EntityFilter::containsTagMap)) {
			badRequestMessages
					.add("on of the provided tags was not valid; the following tags are allowed: "
							+ EntityFilter.TAG_MAP.keySet().stream()
									.collect(Collectors.joining(", ")));
		}

		if (!badRequestMessages.isEmpty()) {
			return new JsonResponseBuilder(ResponseStatus.BAD_REQUEST)
					.addStringMessages(badRequestMessages).build();
		}

		Predicate<Entity> nodeFilter = EntityFilter
				.containsAnyTagString(tagList)
				.and(EntityFilter::hasOpeningHours);

		NearbySearchRequest nearbySearchRequest = nearbySearchHelper
				.createNearbySearchRequestBuilder(startPoint, now)
				.setPredicate(nodeFilter).setMaxResults(maxResults)
				.setMaxDistance(maxDistance).setTimeBuffer(timeBuffer)
				.setEarliestTime(earliestTime).setLatestTime(latestTime)
				.build();

		logger.debug("request: " + request.toString());
		StopWatch sw = new StopWatch();
		sw.start();

		NearbySearchResponse nearbySearchResponse = nearbySearchHelper
				.findPar(nearbySearchRequest);

		sw.stop();
		logger.info("executed request in " + sw.toString()
				+ ", number of result(s): "
				+ nearbySearchResponse.getResults().size());

		// TODO Error handling
		if (nearbySearchResponse.getResults().isEmpty()) {
			JsonObject json = Json.createObjectBuilder()
					.add("status", ResponseStatus.NO_REULTS.toString())
					.add("status_code",
							ResponseStatus.NO_REULTS.getHttpStatusCode())
					.add("messages:", "no results were found").build();

			return Response.ok(JsonUtils.toString(json)).build();
		} else {
			JsonObject json = Json.createObjectBuilder()
					.add("status", ResponseStatus.OK.toString())
					.add("status_code", ResponseStatus.OK.getHttpStatusCode())
					.add("results",
							nearbySearchResponse.getResults().stream()
									.map(JsonUtils::toJsonObject)
									.collect(
											JsonCollectors.toJsonArrayBuilder())
									.build())
					.build();

			return Response.ok(JsonUtils.toString(json)).build();
		}
	}

}
