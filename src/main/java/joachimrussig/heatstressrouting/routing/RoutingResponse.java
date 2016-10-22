package joachimrussig.heatstressrouting.routing;

import java.util.List;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.PathWrapper;
import com.graphhopper.routing.Path;

/**
 * A Response created for a {@link RoutingRequest}. It contains the
 * {@code RoutingRequest} performed as well as the created {@link GHRequest} and
 * the corresponding {@link GHResponse} and the {@link Path}s found.
 * 
 * @author Joachim Rußig
 *
 */
public class RoutingResponse {

	final RoutingRequest request;
	final GHRequest ghRequest;
	final GHResponse ghResponse;
	final List<Path> paths;

	public RoutingResponse(RoutingRequest request, GHRequest ghRequest,
			GHResponse ghResponse, List<Path> paths) {
		this.request = request;
		this.ghRequest = ghRequest;
		this.ghResponse = ghResponse;
		this.paths = paths;
	}

	public PathWrapper getBest() {
		return ghResponse.getBest();
	}

	public List<Throwable> getErrors() {
		return ghResponse.getErrors();
	}

	public GHRequest getGhRequest() {
		return ghRequest;
	}

	public GHResponse getGhResponse() {
		return ghResponse;
	}

	public List<Path> getPaths() {
		return paths;
	}

	public RoutingRequest getRequest() {
		return request;
	}

	public boolean hasErrors() {
		return ghResponse.hasErrors();
	}

	@Override
	public String toString() {
		return "RoutingResponse [request=" + request + ", ghRequest="
				+ ghRequest + ", ghResponse=" + ghResponse + ", paths=" + paths
				+ "]";
	}

}
