package joachimrussig.heatstressrouting.waysegments;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.Pair;

/**
 * The {@code WaySegmentId} represents the id of a {@link WaySegment}. A
 * {@code WaySegmentId} consist of the way id and the ids of the tow adjacent
 * nodes.
 * 
 * @author Joachim Rußig
 */
public class WaySegmentId {

	protected final long wayId;
	private final Pair<Long, Long> nodeIds;

	/**
	 * Creates a new {@code WaySegmentId}.
	 * 
	 * @param wayId
	 *            the OSM way id of the {@code WaySegment}
	 * @param nodeIds
	 *            the OSM node ids of the two adjacent nodes
	 */
	public WaySegmentId(long wayId, Pair<Long, Long> nodeIds) {
		this.wayId = wayId;
		this.nodeIds = nodeIds;
	}

	/**
	 * 
	 * @return the OSM way id
	 */
	public long getWayId() {
		return wayId;
	}

	/**
	 * 
	 * @return the OSM node ids of the two adjacent nodes
	 */
	public Pair<Long, Long> getNodeIds() {
		return nodeIds;
	}

	/**
	 * Two {@code WaySegmentId}s are equals, if the've got the same OSM way id
	 * and the the ids of the adjacent nodes are equals.
	 * 
	 * <p>
	 * 
	 * <b>Note:</b> the order of the node ids matters for comparison.
	 *
	 * 
	 * @returns true, if the way ids and the node ids are equale
	 */
	@Override
	public boolean equals(Object other) {
		return other instanceof WaySegmentId
				&& this.wayId == ((WaySegmentId) other).wayId
				&& this.nodeIds.equals(((WaySegmentId) other).nodeIds);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37).append(wayId)
				.append(nodeIds.getLeft()).append(nodeIds.getRight())
				.toHashCode();

	}

	@Override
	public String toString() {
		return "WaySegmentID(wayId = " + wayId + ", nodeIds = " + nodeIds + ")";
	}
}
