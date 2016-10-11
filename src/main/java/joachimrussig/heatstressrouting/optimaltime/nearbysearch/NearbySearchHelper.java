package joachimrussig.heatstressrouting.optimaltime.nearbysearch;

import java.time.LocalDateTime;
import java.util.List;

import com.graphhopper.util.shapes.GHPoint;

import joachimrussig.heatstressrouting.routing.RoutingHelper;

/**
 * Helper Class, that makes is easier to execute a nearby search.
 * 
 * @author joachimrussig
 *
 */
public class NearbySearchHelper {

	private RoutingHelper routingHelper;

	public NearbySearchHelper(RoutingHelper routingHelper) {
		this.routingHelper = routingHelper;
	}

	/**
	 * Performs a nearby search using the specified {@link NearbySearchRequest}.
	 * The results are ranked according to {@code scoreFunction}.
	 * 
	 * @param request
	 *            the request to perform
	 * @param parallel
	 *            if {@code true}, the search is executed in parallel
	 * @return
	 */
	public NearbySearchResponse find(NearbySearchRequest request,
			boolean parallel) {
		List<NearbySearchResult> results = NearbySearch.find(request.getStart(),
				request.getPredicate(), request.getNow(),
				request.getMaxResults(), request.getMaxDistance(),
				request.getFinder(), request.getScoreFunction(), parallel);
		return new NearbySearchResponse(request, results);
	}

	/**
	 * Performs a nearby search using the specified {@link NearbySearchRequest}.
	 * The results are ranked according to {@code scoreFunction}.
	 * 
	 * @param request
	 *            the request to perform
	 * @return
	 */
	public NearbySearchResponse find(NearbySearchRequest request) {
		List<NearbySearchResult> results = NearbySearch.find(request.getStart(),
				request.getPredicate(), request.getNow(),
				request.getMaxResults(), request.getMaxDistance(),
				request.getFinder(), request.getScoreFunction());
		return new NearbySearchResponse(request, results);
	}

	/**
	 * Performs a nearby search using the specified {@link NearbySearchRequest}.
	 * The results are ranked according to {@code scoreFunction}.
	 * 
	 * @param request
	 *            the request to perform
	 * @return
	 */
	public NearbySearchResponse findPar(NearbySearchRequest request) {
		List<NearbySearchResult> results = NearbySearch.findPar(
				request.getStart(), request.getPredicate(), request.getNow(),
				request.getMaxResults(), request.getMaxDistance(),
				request.getFinder(), request.getScoreFunction());
		return new NearbySearchResponse(request, results);
	}

	public NearbySearchRequestBuilder createNearbySearchRequestBuilder(
			GHPoint start, LocalDateTime now) {
		return new NearbySearchRequestBuilder(this.routingHelper, start, now);
	}

	public RoutingHelper getRoutingHelper() {
		return routingHelper;
	}

	public void setRoutingHelper(RoutingHelper routingHelper) {
		this.routingHelper = routingHelper;
	}

}
