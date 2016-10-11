package joachimrussig.heatstressrouting.optimaltime.nearbysearch;

import java.util.List;

/**
 * The Response created while performing a {@link NearbySearchRequest}.
 * 
 * @author Joachim Rußig
 *
 */
public class NearbySearchResponse {

	private final NearbySearchRequest request;
	private final List<NearbySearchResult> results;

	/**
	 * Creates a new {@code NearbySearchResponse}.
	 * 
	 * @param request
	 *            the {@link NearbySearchRequest} used to create this response
	 * @param results
	 *            the {@link NearbySearchResult}s found
	 */
	public NearbySearchResponse(NearbySearchRequest request,
			List<NearbySearchResult> results) {
		this.request = request;
		this.results = results;
	}

	public List<NearbySearchResult> getResults() {
		return results;
	}

	public NearbySearchRequest getRequest() {
		return request;
	}

	public int numberOfResults() {
		return this.results.size();
	}

	@Override
	public String toString() {
		return "NearbySearchResponse [#results=" + results.size() + ", results="
				+ results + "]";
	}

}
