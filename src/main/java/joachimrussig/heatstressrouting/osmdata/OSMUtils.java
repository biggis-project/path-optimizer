package joachimrussig.heatstressrouting.osmdata;

import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.DistanceCalcEarth;
import com.graphhopper.util.shapes.GHPoint;
import org.jooq.lambda.Seq;
import org.openstreetmap.osmosis.core.domain.v0_6.Bound;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;

import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

/**
 * A utility class, that defines some function useful to deal with OSM data.
 * 
 * @author Joachim Rußig
 */
public class OSMUtils {

	static DistanceCalc dc = new DistanceCalcEarth();

	private OSMUtils() {
	}

	/**
	 * Computes the haversine great circle distance between {@code node1} and
	 * {@code node2}.
	 * <p>
	 * 
	 * @see com.graphhopper.util.DistanceCalcEarth#calcDist(double, double,
	 *      double, double)
	 * @param node1
	 * @param node2
	 * @return haversine great circle distance between {@code node1} and
	 *         {@code node2}
	 */
	public static double distance(Node node1, Node node2) {
		return dc.calcDist(node1.getLatitude(), node1.getLongitude(),
				node2.getLatitude(), node2.getLongitude());
	}


	/**
	 * Computes the haversine great circle distance between {@code p} and
	 * {@code n}.
	 * <p>
	 * 
	 * @see com.graphhopper.util.DistanceCalcEarth#calcDist(double, double,
	 *      double, double)
	 * @param p
	 * @param n
	 * @return haversine great circle distance between {@code n} and {@code p}
	 */
	static double distance(GHPoint p, Node n) {
		return dc.calcDist(p.getLat(), p.getLon(), n.getLatitude(),
				n.getLongitude());
	}

	/**
	 * Computes the cumulated distance between the nodes in {@code nodes}.
	 * 
	 * @see OSMUtils#distance(Node node1, Node node2)
	 * @param nodes
	 *            list of nodes
	 * @return sum of the distances between (n0,n1),(n1,n2),...,(nm-1,nm)
	 */
	public static OptionalDouble distance(List<Node> nodes) {
		return Seq.seq(nodes).sliding(2).map(n -> {
			List<Node> nodeList = n.collect(Collectors.toList());
			if (nodeList.size() == 2) {
				return OSMUtils.distance(nodeList.get(0), nodeList.get(1));
			} else {
				return null;
			}
		}).sum().filter(Objects::nonNull).map(OptionalDouble::of)
				.orElse(OptionalDouble.empty());
	}

	/**
	 * Returns the coordinates of the OSM node {@code node} as a {@link GHPoint}
	 * .
	 * 
	 * @param node
	 * @return the coordinates of the OSM node {@code node} as a {@code GHPoint}
	 */
	public static GHPoint getGHPoint(Node node) {
		return new GHPoint(node.getLatitude(), node.getLongitude());
	}
	
	/**
	 * Checks if {@code point} is within the bounding box {@code bbox}.
	 * 
	 * @param point
	 * @param bbox
	 * @return true, if {@code point} is within the bounding box (exclusive)
	 */
	public static boolean withinBoundingBox(GHPoint point, Bound bbox) {
		double lat = point.getLat();
		double lon = point.getLon();
		return lat > bbox.getBottom() && lat < bbox.getTop()
				&& lon > bbox.getLeft() && lon < bbox.getBottom();
	}

	/**
	 * 
	 * @return the {@link DistanceCalc} instance used to calculate the
	 *         distances.
	 */
	public static DistanceCalc getDc() {
		return dc;
	}

}
