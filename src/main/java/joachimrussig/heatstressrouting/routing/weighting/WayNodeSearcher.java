package joachimrussig.heatstressrouting.routing.weighting;

import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PointList;
import joachimrussig.heatstressrouting.osmdata.OSMData;
import joachimrussig.heatstressrouting.osmdata.OSMUtils;
import org.jooq.lambda.Seq;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper class to find the way nodes, using base and adjacent node and a given
 * list of way points.
 *
 * @author Joachim Rußig
 */
public class WayNodeSearcher {

	private Logger logger = LoggerFactory.getLogger(WayNodeSearcher.class);

	private OSMData osmData;

	/**
	 * Creates a new {@code WayNodeSearcher}.
	 * 
	 * @param osmData
	 *            the OSM data in which the way nodes should be searched
	 */
	public WayNodeSearcher(OSMData osmData) {
		this.osmData = osmData;
	}

	/**
	 * Finds the way nodes for the way specified by {@code wayId} between a base
	 * ({@code baseId}) and adjacent node ({@code adjId}). The way nodes are
	 * identified based on the way id, the ids of the base node and the adjacent
	 * node and a given list of way points. If either base node or adjacent node
	 * are not in the way, a heuristic is applied to find the way nodes, because
	 * GraphHopper sometimes returns the wrong OSM id for the base respectively
	 * adjacent node.
	 *
	 * @param wayId
	 *            the id of the OSM way
	 * @param points
	 *            list of points (lat, long) between the nodes specified by
	 *            {@code baseId} and {@code adjId}
	 * @param baseId
	 *            the OSM id of the base node
	 * @param adjId
	 *            the OSM id of the adjacent node (Optional)
	 * @return a list of the way nodes or {@code null} if non is found
	 *
	 */
	public List<Node> getWayNodes(long wayId, PointList points, long baseId,
			Long adjId, EdgeIteratorState edgeState) {

		List<Node> wayNodes = osmData.getWayNodes(wayId);

		// Check if baseId respectively adjId are part of the OSM way specified
		// by wayId
		boolean containsBaseId = OSMData.contains(wayNodes, baseId);
		boolean containsAdjId = adjId != null
				&& OSMData.contains(wayNodes, adjId);

		if (!containsBaseId && containsAdjId) {
			// The baseId is wrong but we've got the correct ajdId so we just
			// swap them.
			long tmp = baseId;
			baseId = adjId;
			adjId = tmp;
		} else if (!containsBaseId) {
			logger.debug("baseNode " + baseId + " and adjNode " + adjId
					+ " are not in wayNodes (wayId = " + wayId + ")");
			return null;
		}

		// As a way can be cyclic or either base node or adjacent node are wrong
		// there are to possible paths from base node to adjacent node. But
		// because GraphHopper doesn't provide any reliable
		// information in which direction we have to search, we have to search
		// in both directions.
		SearchWayNodesResult forward = searchWayNodes(wayId, points, false,
				baseId, adjId);
		SearchWayNodesResult backward = searchWayNodes(wayId, points, true,
				baseId, adjId);

		// We have to consider different case to determine which solution we
		// return.
		if (forward.exactMatch && backward.exactMatch) {
			if (adjId != null && baseId == adjId)
				// We found a cycle from baseNode to adjNode, so it's regardless
				// which we return.
				return forward.nodes;

			// We found two paths with an exact match, so we are returning that
			// with the smaller average deviation
			if (forward.avgDeviation <= backward.avgDeviation)
				return forward.nodes;
			else
				return backward.nodes;

		} else if (forward.exactMatch) {
			// only forward is an exact match
			return forward.nodes;

		} else if (backward.exactMatch) {
			// only backward is an exact match
			return backward.nodes;

		} else if (forward.nodes.size() == points.size()
				&& backward.nodes.size() != points.size()) {
			// No exact match, but only forward has the exact number of nodes,
			// so we return that.
			return forward.nodes;

		} else if (forward.nodes.size() == points.size()
				&& backward.nodes.size() == points.size()) {
			// Both, forward and backward, have the correct number of nodes, so
			// we return that with the smaller average deviation
			if (forward.avgDeviation <= backward.avgDeviation)
				return forward.nodes;
			else
				return backward.nodes;

		} else if (backward.nodes.size() == points.size()) {
			// No exact match, but only backward has the exact number of nodes,
			// so we return that
			return backward.nodes;

		} else {
			// Neither forward nor backward have the correct number of way
			// nodes, so no solution found
			logger.debug("no match found");
			return null;
		}
	}

	/**
	 * Helper function that searches for the wayNodes in way {@code wayId}
	 * between {@code baseId} and {@code ajdId} starting at {@code baseId}.
	 * 
	 * @param wayId
	 *            the OSM way id of the way to search in
	 * @param points
	 *            coordinates of the pillar nodes representing the way geometry
	 *            between base node and adjacent node
	 * @param reverse
	 *            indicates whether should be searched in reverse direction
	 * @param baseId
	 *            the OSM id of the base node
	 * @param adjId
	 *            the OSM id of the adjacent node, can be {@code null}
	 * @return result of the way node search
	 */
	private SearchWayNodesResult searchWayNodes(long wayId, PointList points,
			boolean reverse, long baseId, Long adjId) {
		final int n = points.size();

		List<Node> wayNodes = osmData.getWayNodes(wayId);

		List<Node> res = new ArrayList<>(n);
		double deviation = 0;
		boolean exactMatch = false;

		Seq<Node> nodes = Seq.seq(wayNodes);

		if (reverse)
			nodes = nodes.reverse();

		if (osmData.isCyclicWay(wayId))
			nodes = nodes.cycle(2);

		// Find the base node in the nodes
		List<Node> iterNodes = nodes.skipUntil(node -> node.getId() == baseId)
				.collect(Collectors.toList());
		Iterator<Node> iter = iterNodes.iterator();

		int i = 0;
		while (i < n && iter.hasNext()) {
			Node node = iter.next();

			// deviation between the way node and the way point
			deviation += OSMUtils.getDc().calcDist(points.getLat(i),
					points.getLon(i), node.getLatitude(), node.getLongitude());
			res.add(node);

			// We found an exact match, because we've found the correct number
			// of way nodes and the id of the first and last node in both lists
			// are equals
			if (adjId != null && adjId == node.getId() && i == (n - 1))
				exactMatch = true;

			i++;
		}

		return new SearchWayNodesResult(exactMatch, (deviation / n), res);
	}

	/**
	 * A helper class that represents the result of a way node search.
	 * 
	 * @author Joachim Rußig
	 *
	 */
	private static class SearchWayNodesResult {
		/**
		 * Indicates that the found way nodes have the correct size and the base
		 * node id and the adjacent node ids are correct.
		 */
		boolean exactMatch;
		/**
		 * Average deviation of the found nodes to the pillar nodes in the point
		 * list.
		 */
		double avgDeviation;
		List<Node> nodes;

		/**
		 * Creates a new {@code SearchWayNodesResult}
		 * 
		 * @param exactMatch
		 *            indicates if a exact match was found
		 * @param avgDeviation
		 *            average deviation between the found nodes and the nodes in
		 *            the point list
		 * @param nodes
		 *            list of the found nodes
		 */
		SearchWayNodesResult(boolean exactMatch, double avgDeviation,
				List<Node> nodes) {
			this.exactMatch = exactMatch;
			this.avgDeviation = avgDeviation;
			this.nodes = nodes;
		}
	}

	/**
	 * 
	 * @return the OSM data
	 */
	public OSMData getOsmData() {
		return osmData;
	}

	/**
	 * Sets the OSM data
	 * 
	 * @param osmData
	 *            the OSM data to set
	 */
	public void setOsmData(OSMData osmData) {
		this.osmData = osmData;
	}
}
