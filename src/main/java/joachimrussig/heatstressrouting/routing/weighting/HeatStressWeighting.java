package joachimrussig.heatstressrouting.routing.weighting;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.jooq.lambda.Seq;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.weighting.AbstractWeighting;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.DistanceCalcEarth;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PointList;

import joachimrussig.heatstressrouting.osmdata.OSMData;
import joachimrussig.heatstressrouting.routing.HeatStressGraphHopper;
import joachimrussig.heatstressrouting.waysegments.WaySegment;
import joachimrussig.heatstressrouting.waysegments.WaySegmentId;
import joachimrussig.heatstressrouting.waysegments.WaySegments;
import joachimrussig.heatstressrouting.weatherdata.WeatherData;

/**
 * An abstract {@link Weighting} that provides common functionalities used for
 * the heat stress routing, like the identification of the way segments.
 * 
 * @author Joachim Rußig
 */
public abstract class HeatStressWeighting extends AbstractWeighting {

	private final Logger logger = LoggerFactory
			.getLogger(HeatStressWeighting.class);

	private final HeatStressGraphHopper hopper;
	private final WaySegments segments;
	private final WayNodeSearcher wayNodeSearcher;
	private final LocalDateTime time;

	protected DistanceCalc dc = new DistanceCalcEarth();

	// see com.graphhopper.routing.QueryGraph
	private int mainEdges;
	private int mainNodes;

	public HeatStressWeighting(FlagEncoder flagEncoder,
			HeatStressGraphHopper hopper, WaySegments segments,
			LocalDateTime time) {
		super(flagEncoder);
		this.hopper = hopper;
		this.segments = segments;
		this.time = time;
		this.mainEdges = hopper.getGraphHopperStorage().getAllEdges()
				.getMaxId();
		this.mainNodes = hopper.getGraphHopperStorage().getNodes();
		this.wayNodeSearcher = new WayNodeSearcher(hopper.getOsmData());
	}

	@Override
	public double calcWeight(EdgeIteratorState edgeState, boolean reverse,
			int prevOrNextEdgeId) {

		// if the edge is virtual, there are no data available so we just return
		// the distance
		//
		// https://github.com/graphhopper/graphhopper/blob/master/docs/core/low-level-api.md
		if (isVirtualEdge(edgeState.getEdge()))
			return edgeState.getDistance();

		final OSMData osmData = hopper.getOsmData();
		final WeatherData weatherData = hopper.getWeatherData();
		final LocalDateTime timePoint = this.time;

		long wayId = hopper.getOSMWay(edgeState.getEdge());

		long baseNodeId = hopper.getOSMNode(edgeState.getBaseNode());
		long adjNodeId = hopper.getOSMNode(edgeState.getAdjNode());

		if (!osmData.contains(baseNodeId) || !osmData.contains(adjNodeId)) {
			return Double.MAX_VALUE;
		}

		// compute the segments of the current edge
		List<WaySegmentId> edgeSegments = getEdgeSegments(wayId, baseNodeId,
				adjNodeId, edgeState);

		// if there is no data available we return just the distance
		if (edgeSegments.isEmpty())
			return edgeState.getDistance();

		if (logger.isDebugEnabled())
			edgeSegments.forEach(s -> {
				if (!segments.getSegment(s, timePoint.toLocalTime()).isPresent()
						&& !s.getNodeIds().getLeft()
								.equals(s.getNodeIds().getRight()))
					logger.debug("Missing " + s + ", distance = "
							+ edgeState.getDistance() + ", segments.size() = "
							+ segments.getSegments().size());
			});

		// compute the weight of the current edge
		double weight = computeWeight(edgeSegments, timePoint);

		if (weight < 0) {
			logger.error("weight is negative! weight = " + weight
					+ ", distance = " + edgeState.getDistance()
					+ ", timePoint = " + timePoint + ", wayId = " + wayId
					+ ", baseId = " + baseNodeId + ", adjId = " + adjNodeId
					+ ", weatherData = "
					+ weatherData.getWeatherRecord(timePoint)
					+ ", distanceEdgeSegments = "
					+ edgeSegments.stream()
							.map(id -> segments.getSegment(id,
									timePoint.toLocalTime()))
							.filter(Optional::isPresent).map(Optional::get)
							.mapToDouble(WaySegment::getDistance).sum()
					+ ", edgeSegments = " + edgeSegments);
			edgeSegments.stream()
					.map(id -> segments.getSegment(id, timePoint.toLocalTime()))
					.forEach(s -> System.out.println("segment = " + s));

			throw new IllegalStateException(
					"negative edge weight: edge from " + baseNodeId + " to "
							+ adjNodeId + " (wayId = " + wayId + ")");
		}
		return weight;
	}

	/**
	 * Computes the weight for the given way segment.
	 * 
	 * @param segment
	 *            the way segment to compute the weight for
	 * @param time
	 *            to compute the weight for
	 * @return the computed weight
	 */
	protected abstract double computeSegmentWeight(WaySegment segment,
			LocalDateTime time);

	/**
	 * Computes the weight of the segments identified by {@code segmentsId}.
	 * 
	 * @param segmentsIds
	 *            the segments to compute the weight for
	 * @param time
	 *            to compute the weight for
	 * @return the weights of the given list of way segments
	 */
	protected double computeWeight(List<WaySegmentId> segmentsIds,
			LocalDateTime time) {
		return segmentsIds.stream()
				.map(s -> getSegments().getSegment(s, time.toLocalTime()))
				.filter(Optional::isPresent).map(Optional::get)
				.mapToDouble(s -> computeSegmentWeight(s, time)).sum();
	}

	/**
	 * Identifies all subways including those between pillar nodes and returns
	 * the way segments of all of them.
	 * 
	 * @param wayId
	 *            if of the OSM way
	 * @param baseNodeId
	 *            id of the base node
	 * @param adjNodeId
	 *            id of the adjacent node
	 * @param edgeState
	 * @return the ids of all way segments of the way {@code wayId} between the
	 *         base node {@code baseNodeId} and the adjacent node
	 *         {@code adjNodeId}
	 */
	List<WaySegmentId> getEdgeSegments(long wayId, long baseNodeId,
			long adjNodeId, EdgeIteratorState edgeState) {

		OSMData osmData = hopper.getOsmData();

		// if the edge is virtual there are no data available because there is
		// now corresponding OSM way so we just return an empty list
		if (isVirtualEdge(edgeState.getEdge())) {
			logger.debug("virtual edge " + edgeState.getEdge() + ", distance = "
					+ edgeState.getDistance());

			return new ArrayList<>();
		}

		PointList wayPoints = edgeState.fetchWayGeometry(3);
		List<Node> wayNodes = osmData.getWayNodes(wayId);
		List<Long> wayNodeIds = wayNodes.stream().map(Node::getId)
				.collect(Collectors.toList());

		Long optAdjId = wayNodeIds.contains(adjNodeId) ? adjNodeId : null;

		// find the way nodes between the base node and the adjacent node
		List<Node> edgeNodes = wayNodeSearcher.getWayNodes(wayId, wayPoints,
				baseNodeId, optAdjId, edgeState);

		if (edgeNodes.size() != wayPoints.size()) {
			logger.debug("different number of wayNodes found: edgeNodes"
					+ edgeNodes + "\nwayPoints = " + wayPoints);
			return new ArrayList<>();
		}

		return Seq.seq(edgeNodes).sliding(2).map(s -> {
			List<Node> l = s.collect(Collectors.toList());
			return new WaySegmentId(wayId,
					Pair.of(l.get(0).getId(), l.get(1).getId()));
		}).collect(Collectors.toList());
	}

	public WeatherData getHeatStress() {
		return hopper.getWeatherData();
	}

	public HeatStressGraphHopper getHopper() {
		return hopper;
	}

	@Override
	public double getMinWeight(double currDistToGoal) {
		return currDistToGoal;
	}

	@Override
	public abstract String getName();

	public WaySegments getSegments() {
		return segments;
	}

	public LocalDateTime getTime() {
		return time;
	}

	/**
	 * Checks, if the edge {@code edgeId} is a virtual edge.
	 * 
	 * @see com.graphhopper.routing.QueryGraph#isVirtualEdge(int)
	 * @param edgeId
	 *            the edge id to check
	 * @return true, if {@code edgeId} is a virtual edge
	 */
	protected boolean isVirtualEdge(int edgeId) {
		return edgeId >= mainEdges;
	}

	/**
	 * Checks, if the node {@code nodeId} is a virtual node.
	 * 
	 * @see com.graphhopper.routing.QueryGraph#isVirtualNode(int)
	 * @param edgeId
	 *            the node id to check
	 * @return true, if {@code nodeId} is a virtual node
	 */
	protected boolean isVirtualNode(int nodeId) {
		return nodeId >= mainNodes;
	}
}
