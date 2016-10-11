package joachimrussig.heatstressrouting.routing;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.reader.DataReader;
import com.graphhopper.reader.OSMNode;
import com.graphhopper.reader.OSMReader;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.Weighting;
import com.graphhopper.routing.util.WeightingMap;
import com.graphhopper.storage.DataAccess;
import com.graphhopper.storage.Directory;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.util.BitUtil;

import joachimrussig.heatstressrouting.osmdata.OSMData;
import joachimrussig.heatstressrouting.routing.weighting.HeatStressWeightingHeatIndex;
import joachimrussig.heatstressrouting.routing.weighting.HeatStressWeightingHeatIndexWeighted;
import joachimrussig.heatstressrouting.routing.weighting.HeatStressWeightingTemperature;
import joachimrussig.heatstressrouting.routing.weighting.WeightingType;
import joachimrussig.heatstressrouting.waysegments.WaySegments;
import joachimrussig.heatstressrouting.weatherdata.WeatherData;

/**
 * The {@code HeatStressGraphHopper} class extends the {@link GraphHopper} with
 * the functionality to find a route with a minimal heat stress value.
 * 
 * @author Joachim Rußig
 */
public class HeatStressGraphHopper extends GraphHopper {

	private final Logger logger = LoggerFactory
			.getLogger(HeatStressGraphHopper.class);

	private WeatherData weatherData;

	private WaySegments segments;
	private OSMData osmData;

	private double weightDistance = 0.5;
	private double weightThermalComfot = 0.5;

	private String[] weihtingsRequiringTime = Arrays
			.stream(WeightingType.values())
			.filter(w -> w != WeightingType.SHORTEST)
			.map(WeightingType::toString).toArray(String[]::new);

	// mapping of internal edge/node ID to OSM way/node ID
	// See: https://github.com/karussell/graphhopper-osm-id-mapping/
	private DataAccess edgeMapping;
	private DataAccess nodeMapping;
	private BitUtil bitUtil;

	public HeatStressGraphHopper() {
		super();
	}

	@Override
	public Weighting createWeighting(WeightingMap weightingMap,
			FlagEncoder encoder) {

//		logger.debug("weightingMap = " + weightingMap.toString());
//		logger.debug("encoder = " + encoder.toString());
		
		String weighting = weightingMap.getWeighting();

		LocalDateTime time = null;
		if (Arrays.stream(weihtingsRequiringTime)
				.anyMatch(w -> w.equalsIgnoreCase(weighting))) {

			if (weightingMap.has("time"))
				time = LocalDateTime.parse(weightingMap.get("time", null));
			else
				throw new IllegalStateException("for weighting type '"
						+ weighting
						+ "' weighting map must contain an valid time entry");
		}

		if (weighting.equalsIgnoreCase(HeatStressWeightingTemperature.NAME)) {

			return new HeatStressWeightingTemperature(encoder, this, segments,
					time);

		} else if (weighting
				.equalsIgnoreCase(HeatStressWeightingHeatIndex.NAME)) {

			return new HeatStressWeightingHeatIndex(encoder, this, segments,
					time);

		} else if (weighting
				.equalsIgnoreCase(HeatStressWeightingHeatIndexWeighted.NAME)) {
			HeatStressWeightingHeatIndexWeighted hw = new HeatStressWeightingHeatIndexWeighted(
					encoder, this, segments, time);
			hw.setWeights(this.weightDistance, this.weightThermalComfot);
			return hw;
		} else {
			return super.createWeighting(weightingMap, encoder);
		}
	}

	/**
	 * Calculates the path from specified request visiting the specified
	 * locations.
	 * 
	 * @param request
	 *            the {@link GHRequest}
	 * @param time
	 *            the point in time to find the route for
	 * @return the response with the route and possible errors
	 */
	public GHResponse route(GHRequest request, LocalDateTime time) {
		// add the time to the weightingMap which is passed to the
		// createWeighting method
		request.getHints().put("time", time.toString());
		return super.route(request);
	}

	/**
	 * Calculates the path from specified request visiting the specified
	 * locations.
	 * 
	 * @param request
	 * @return the {@link GHResponse} and a list of the found paths
	 */
	public Pair<GHResponse, List<Path>> routePaths(GHRequest request) {
		GHResponse response = new GHResponse();
		List<Path> paths = calcPaths(request, response);
		
		return Pair.of(response, paths);
	}

	/**
	 * Calculates the path from specified request visiting the specified
	 * locations.
	 * 
	 * @param request
	 * @param time
	 * @return the {@link GHResponse} and a list of the found paths
	 */
	public Pair<GHResponse, List<Path>> routePaths(GHRequest request,
			LocalDateTime time) {
		// add the time to the weightingMap which is passed to the
		// createWeighting method
		request.getHints().put("time", time.toString());

		return routePaths(request);
	}

	@Override
	public boolean load(String graphHopperFolder) {
		boolean loaded = super.load(graphHopperFolder);

		Directory dir = getGraphHopperStorage().getDirectory();
		this.bitUtil = BitUtil.get(dir.getByteOrder());
		this.edgeMapping = dir.find("edge_mapping");
		this.nodeMapping = dir.find("node_mapping");

		if (loaded) {
			this.edgeMapping.loadExisting();
			this.nodeMapping.loadExisting();
		}

		return loaded;
	}

	// Override the createReader method to store the mapping of internal
	// edge/node ID to OSM way/node ID
	// See: https://github.com/karussell/graphhopper-osm-id-mapping/
	@Override
	protected DataReader createReader(GraphHopperStorage ghStorage) {
		OSMReader reader = new OSMReader(ghStorage) {

			private Set<Long> osmNodeIds; // temporary store the added OSM nodes
			private int numberStoredWayIds = 0;
			private int maxStoredInternalWayId = 0;

			{
				osmNodeIds = new HashSet<>();
				edgeMapping.create(1000);
				nodeMapping.create(1000);
			}

			// hook the store OsmWayID function to persist the mapping of the
			// internal edge IDs to the OSM way IDs
			@Override
			protected void storeOsmWayID(int edgeId, long osmWayId) {
				super.storeOsmWayID(edgeId, osmWayId);

				long pointer = 8L * edgeId;
				edgeMapping.ensureCapacity(pointer + 8L);

				edgeMapping.setInt(pointer, bitUtil.getIntLow(osmWayId));
				edgeMapping.setInt(pointer + 4, bitUtil.getIntHigh(osmWayId));

				if (edgeId > maxStoredInternalWayId)
					maxStoredInternalWayId = edgeId;
				numberStoredWayIds++;
			}

			// Required to store the OSM node ID so it can be persisted in the
			// finishedReading() method.
			//
			// An inelegant hack because addNode is 'default' and can not be
			// overridden. Since getElevation is 'protected' and called on every
			// non empty node, we're using that as a hook to store the mapping
			// of the internal nodes to the corresponding OSM IDs.
			//
			@Override
			protected double getElevation(OSMNode node) {
				double res = super.getElevation(node);

				// Store the OSM Node ID so it can be persisted in
				// finishedReading()
				this.osmNodeIds.add(node.getId());

				return res;
			}

			// helper method to store the OSM node nodeIds in the nodeMapping
			private void storeOsmNodeId(int internalID, long osmID) {
				long pointer = 8L * internalID;

				nodeMapping.ensureCapacity(pointer + 8L);

				nodeMapping.setInt(pointer, bitUtil.getIntLow(osmID));
				nodeMapping.setInt(pointer + 4, bitUtil.getIntHigh(osmID));
			}

			@Override
			protected void finishedReading() {
				// Persist the osmNodeId collected in getElevation().
				// We only get the tower nodes, but that should be enough to
				// find the way segments later.
				int i = 0;
				int maxInternalId = 0;
				for (long osmNodeId : osmNodeIds) {
					int internalId = super.getInternalNodeIdOfOsmNode(
							osmNodeId);
					if (internalId != OSMReader.EMPTY) {
						storeOsmNodeId(internalId, osmNodeId);
						if (internalId > maxInternalId)
							maxInternalId = internalId;
						i++;
					}
				}
				logger.debug("added " + i + " nodes to nodeMapping (collected "
						+ osmNodeIds.size() + " nodes, maxInternalId = "
						+ maxInternalId + ", maxStoredInternalWayId = "
						+ maxStoredInternalWayId + ", numberStoredWayIds = "
						+ numberStoredWayIds + ", nodeMap.size() = "
						+ super.getNodeMap().getSize() + ")");

				super.finishedReading();

				osmNodeIds = null;

				edgeMapping.flush();
				nodeMapping.flush();
			}
		};

		return initOSMReader(reader);
	}

	/**
	 * 
	 * @param internalEdgeId
	 * @return the OSM Way id which corresponds to the internal edge id
	 *         {@code internalEdgeId}
	 */
	public long getOSMWay(int internalEdgeId) {
		long pointer = 8L * internalEdgeId;
		return bitUtil.combineIntsToLong(edgeMapping.getInt(pointer),
				edgeMapping.getInt(pointer + 4L));
	}

	/**
	 * 
	 * @param internalNodeId
	 * @return the OSM Node id which corresponds to the internal node id
	 *         {@code internalNodeId}
	 */
	public long getOSMNode(int internalNodeId) {
		long pointer = 8L * internalNodeId;
		return bitUtil.combineIntsToLong(nodeMapping.getInt(pointer),
				nodeMapping.getInt(pointer + 4L));
	}

	public WeatherData getWeatherData() {
		return weatherData;
	}

	public void setWeatherData(WeatherData weatherData) {
		this.weatherData = weatherData;
	}

	// public LocalDateTime getTimePoint() {
	// return timePoint;
	// }
	//
	// public void setTimePoint(LocalDateTime timePoint) {
	// this.timePoint = timePoint;
	// }

	public OSMData getOsmData() {
		return osmData;
	}

	public void setOsmData(OSMData osmData) {
		this.osmData = osmData;
	}

	public WaySegments getSegments() {
		return segments;
	}

	public void setSegments(WaySegments segments) {
		this.segments = segments;
	}

	public double getWeightDistance() {
		return weightDistance;
	}

	public void setWeightDistance(double weightDistance) {
		this.weightDistance = weightDistance;
	}

	public double getWeightThermalComfot() {
		return weightThermalComfot;
	}

	public void setWeightThermalComfot(double weightThermalComfot) {
		this.weightThermalComfot = weightThermalComfot;
	}

	public void setWeights(double weightDistance, double weightThermalComfot) {
		this.weightDistance = weightDistance;
		this.weightThermalComfot = weightThermalComfot;
	}

}
