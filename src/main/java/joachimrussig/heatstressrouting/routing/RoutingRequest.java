package joachimrussig.heatstressrouting.routing;

import java.time.LocalDateTime;
import java.util.Locale;

import com.graphhopper.routing.util.FlagEncoderFactory;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.shapes.GHPoint;

import joachimrussig.heatstressrouting.routing.weighting.WeightingType;

/**
 * 
 * 
 * @author Joachim Rußig
 *
 */
public class RoutingRequest {


	private final GHPoint start;
	private final GHPoint destination;
	private final WeightingType weightingType;
	private final LocalDateTime time;

	private final String routingAlgorithm;
	private final String encodingManager;
	private final Locale locale;

	protected RoutingRequest(GHPoint start, GHPoint destination,
			WeightingType weightingType, LocalDateTime time,
			String routingAlgorithm, String encodingManager, Locale locale) {
		this.start = start;
		this.destination = destination;
		this.weightingType = weightingType;
		this.time = time;
		this.routingAlgorithm = routingAlgorithm;
		this.encodingManager = encodingManager;
		this.locale = locale;
	}

	protected RoutingRequest(GHPoint start, GHPoint destination,
			WeightingType weightingType, LocalDateTime time) {
		this(start, destination, weightingType, time,
				Parameters.Algorithms.DIJKSTRA_BI, FlagEncoderFactory.FOOT,
				Locale.ENGLISH);
	}

	public GHPoint getStart() {
		return start;
	}

	public GHPoint getDestination() {
		return destination;
	}

	public WeightingType getWeightingType() {
		return weightingType;
	}

	public LocalDateTime getTime() {
		return time;
	}

	public String getRoutingAlgorithm() {
		return routingAlgorithm;
	}

	public String getEncodingManager() {
		return encodingManager;
	}

	public Locale getLocale() {
		return locale;
	}

	@Override
	public String toString() {
		return "RoutingRequest [start=" + start + ", destination=" + destination
				+ ", weightingType=" + weightingType + ", time=" + time
				+ ", routingAlgorithm=" + routingAlgorithm
				+ ", encodingManager=" + encodingManager + ", locale=" + locale
				+ "]";
	}

}
