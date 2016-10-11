package joachimrussig.heatstressrouting.webapi;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import joachimrussig.heatstressrouting.HeatStressRouting;
import joachimrussig.heatstressrouting.optimaltime.nearbysearch.NearbySearchHelper;
import joachimrussig.heatstressrouting.routing.HeatStressGraphHopper;
import joachimrussig.heatstressrouting.routing.RoutingHelper;

/**
 * 
 * @author Joachim Rußig
 *
 */
public class ResourceBinder extends AbstractBinder {

	private Logger logger = LoggerFactory.getLogger(ResourceBinder.class);
	
	private RoutingHelper routingHelper = null;
	private NearbySearchHelper nearbySearchHelper = null;

	@Override
	protected void configure() {

		logger.debug("ResourceBinder.configure() called");
		
		// TODO improve error handling
		try {
			String dataDir = getDataDir(this.getClass()).toAbsolutePath()
					.toString();

			File osmFile = Paths.get(dataDir, HeatStressRouting.OSM_FILE_NAME)
					.toFile();
			File weatherDataFile = Paths
					.get(dataDir, HeatStressRouting.WEATHER_FILE_NAME).toFile();
			File waySegmentsFile = Paths
					.get(dataDir, HeatStressRouting.WAY_SEGMENTS_FILE_NAME)
					.toFile();

			HeatStressGraphHopper hopper = RoutingHelper.createHopper(osmFile,
					weatherDataFile, waySegmentsFile);

			RoutingHelper routingHelper = new RoutingHelper(hopper);
			this.routingHelper = routingHelper;
			NearbySearchHelper nearbySearchHelper = new NearbySearchHelper(
					routingHelper);
			this.nearbySearchHelper = nearbySearchHelper;

			bind(routingHelper).to(RoutingHelper.class);
			bind(nearbySearchHelper).to(NearbySearchHelper.class);
		} catch (IOException | URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * A simple helper function that returns the data directory based on the
	 * class passed as {@code clazz}. The data directory is a the route of the
	 * war file.
	 * 
	 * @param clazz
	 * @return
	 * @throws URISyntaxException
	 */
	public static Path getDataDir(Class<?> clazz) throws URISyntaxException {
		URL baseDir = clazz.getClassLoader().getResource("../../");
		Path dataDir = Paths.get(new File(baseDir.toURI()).getAbsolutePath(),
				"data");
		return dataDir;
	}

	public RoutingHelper getRoutingHelper() {
		return routingHelper;
	}

	public void setRoutingHelper(RoutingHelper routingHelper) {
		this.routingHelper = routingHelper;
	}

	public NearbySearchHelper getNearbySearchHelper() {
		return nearbySearchHelper;
	}

	public void setNearbySearchHelper(NearbySearchHelper nearbySearchHelper) {
		this.nearbySearchHelper = nearbySearchHelper;
	}

}
