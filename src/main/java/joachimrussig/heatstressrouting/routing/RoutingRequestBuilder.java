package joachimrussig.heatstressrouting.routing;

import java.time.LocalDateTime;
import java.util.Locale;

import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.shapes.GHPoint;

import joachimrussig.heatstressrouting.routing.weighting.WeightingType;

public class RoutingRequestBuilder {

	private GHPoint start;
	private GHPoint destination;
	private WeightingType weightingType;
	private LocalDateTime time;

	private String routingAlgorithm = AlgorithmOptions.DIJKSTRA_BI;
	private String encodingManager = EncodingManager.FOOT;
	private Locale locale = Locale.ENGLISH;

	protected RoutingRequestBuilder(GHPoint start, GHPoint destination,
			WeightingType weightingType, LocalDateTime time,
			String routingAlgorithm, String encodingManager, Locale locale) {
		this(start, destination, weightingType, time);
		this.routingAlgorithm = routingAlgorithm;
		this.encodingManager = encodingManager;
		this.locale = locale;
	}

	public RoutingRequestBuilder(GHPoint start, GHPoint destination,
			WeightingType weightingType, LocalDateTime time) {
		this.start = start;
		this.destination = destination;
		this.weightingType = weightingType;
		this.time = time;
	}

	/**
	 * 
	 * @param start
	 * @param destination
	 * @return
	 */
	public static RoutingRequestBuilder shortestRoutingRequest(GHPoint start,
			GHPoint destination) {
		return new RoutingRequestBuilder(start, destination,
				WeightingType.SHORTEST, LocalDateTime.now());
	}

	/**
	 * Creates an {@code RoutingRequest} instance based on the set
	 * configuration.
	 * 
	 * @return
	 */
	public RoutingRequest build() {
		return new RoutingRequest(this.start, this.destination,
				this.weightingType, this.time, this.routingAlgorithm,
				this.encodingManager, this.locale);
	}

	/**
	 * Sets the routing algorithm used for routing (e.g.
	 * {@code AlgorithmOptions.DIJKSTRA} or
	 * {@code AlgorithmOptions.DIJKSTRA_BI}).
	 * 
	 * @param routingAlogrithm
	 *            routing algorithm to use; possible values are defined constant
	 *            strings in the {@link AlgorithmOptions}
	 * @return
	 */
	public RoutingRequestBuilder setRoutingAlgorithm(String routingAlogrithm) {
		// TOOD verify algo option (see AlgorithmOptions)
		this.routingAlgorithm = routingAlogrithm;
		return this;
	}

	/**
	 * Sets the encoding manager to use (e.g. {@code EncodingManager.FOOT}). See
	 * {@link EncodingManager} for predefined values.
	 * 
	 * @param encodingManager
	 *            the encoding manager to use
	 * @return
	 */
	public RoutingRequestBuilder setEncodingManager(String encodingManager) {
		// TODO verify encoding manager
		this.encodingManager = encodingManager;
		return this;
	}

	/**
	 * Sets the locale used e.g. for the instructions.
	 * 
	 * @param locale
	 * @return
	 */
	public RoutingRequestBuilder setLocale(Locale locale) {
		this.locale = locale;
		return this;
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

}
