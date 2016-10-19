package joachimrussig.heatstressrouting.osmdata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.tuple.Pair;
import org.openstreetmap.osmosis.core.domain.v0_6.Bound;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.task.v0_6.RunnableSource;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.graphhopper.util.StopWatch;

import crosby.binary.osmosis.OsmosisReader;

/**
 * A class to read a OSM file thats is either in the OSM-XML or the OSM-PBF file
 * format.
 * 
 * @author Joachim Rußig
 */
public class OSMFileReader {

	private static final Logger logger = LoggerFactory
			.getLogger(OSMFileReader.class);

	/**
	 * Creates a new {@code OSMFileReader}.
	 */
	public OSMFileReader() {
	}

	/**
	 * Reads the OSM file specified by {@code file} and returns a
	 * {@link OSMData} object.
	 * 
	 * @param file
	 *            OSM file to read
	 * @return the content of the OSM file as {@code OSMData} object
	 * @throws IOException
	 *             if an error occoured while reading {@code file}
	 */
	public OSMData read(File file) throws IOException {
		Pair<MultiValuedMap<Long, Entity>, Bound> osmData = readOsmFile(file);
		return new OSMData(osmData.getLeft(), osmData.getRight());
	}

	/**
	 * Reads the specified {@code file} to a
	 * {@code MultiValuedMap<Long, Entity>}.
	 * 
	 * @see "http://forum.openstreetmap.org/viewtopic.php?id=15037"
	 * @param file
	 *            the OSM file to read
	 * @return the content of the {@code file} as a
	 *         {@code MultiValuedMap<Long, Entity>}, where the key is the OSM id
	 *         of the entity and the value is an {@link Entity} and if present the bounding
	 *         box as {@link Bound} and {@code null} otherwise
	 * @throws FileNotFoundException
	 */
	public static Pair<MultiValuedMap<Long, Entity>, Bound> readOsmFile(
			File file) throws FileNotFoundException {
		// See: http://forum.openstreetmap.org/viewtopic.php?id=15037

		CollectorSink collectorSink = new CollectorSink();

		logger.debug("OSM File: " + file.getName() + " (exists = "
				+ file.exists() + ")");

		boolean pbf = false;
		CompressionMethod compression = CompressionMethod.None;

		if (file.getName().endsWith(".pbf")) {
			pbf = true;
		} else if (file.getName().endsWith(".gz")) {
			compression = CompressionMethod.GZip;
		} else if (file.getName().endsWith(".bz2")) {
			compression = CompressionMethod.BZip2;
		}

		logger.debug(
				"pbf = " + pbf + "; compression = " + compression.toString());

		RunnableSource reader;

		if (pbf) {
			reader = new OsmosisReader(new FileInputStream(file));
		} else {
			reader = new XmlReader(file, false, compression);
		}

		reader.setSink(collectorSink);

		logger.info("Start reading ...");
		StopWatch sw = new StopWatch().start();

		reader.run();

		logger.info("done (" + sw.stop().getTime() + " ms )");

		return Pair.of(collectorSink.getEntities(),
				collectorSink.getBoundingBox());
	}

}
