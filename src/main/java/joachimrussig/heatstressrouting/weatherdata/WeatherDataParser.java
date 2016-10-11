package joachimrussig.heatstressrouting.weatherdata;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.TreeMap;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code WeatherDataParser} parses the hourly weather data provided as
 * comma separated values file (csv) and returns a {@code WeatherData} instance.
 * <p>
 * The csv file must have the format specified in:
 * ftp://ftp-cdc.dwd.de/pub/CDC/observations_germany/climate/hourly/
 * air_temperature/recent/DESCRIPTION_obsgermany_climate_hourly_tu_recent_en.pdf
 * (accessed on 2016-06-25)
 * 
 * @author Joachim Rußig
 */
public class WeatherDataParser {

	private static Logger logger = LoggerFactory
			.getLogger(WeatherDataParser.class);

	private static final String DATE_COL = "MESS_DATUM";
	private static final String TEMPERATURE_COL = "LUFTTEMPERATUR";
	private static final String RELATIVE_HUMIDITY_COL = "REL_FEUCHTE";
	private static final String DATE_FORMAT = "yyyyMMddHH";
	private static final char DELIMITER = ';';

	/**
	 * Creates a new WeatherDataParser.
	 */
	public WeatherDataParser() {
	}

	/**
	 * Parses the weather data given in the csv file {@code file} and returns a
	 * {@code WeatherData} object.
	 * <p>
	 * The csv file must have the format specified in:
	 * ftp://ftp-cdc.dwd.de/pub/CDC/observations_germany/climate/hourly/
	 * air_temperature/recent/DESCRIPTION_obsgermany_climate_hourly_tu_recent_en
	 * .pdf (accessed on 2016-06-25)
	 * 
	 * @param file
	 *            the csv file to parse
	 * @return the content of the file as {@code WeatherData} object
	 * 
	 * @throws IOException
	 *             if an error occurred while reading {@code file}
	 * @throws NumberFormatException
	 *             if a number cannot be parsed
	 * @throws DateTimeParseException
	 *             if a date cannot be parsed
	 */
	public WeatherData parse(File file) throws IOException {
		return new WeatherData(parseCsvFile(file));
	}

	/**
	 * Parses the weather data given in the csv file {@code file} and returns a
	 * {@code WeatherData} object.
	 * <p>
	 * The csv file must have the format specified in:
	 * ftp://ftp-cdc.dwd.de/pub/CDC/observations_germany/climate/hourly/
	 * air_temperature/recent/DESCRIPTION_obsgermany_climate_hourly_tu_recent_en
	 * .pdf (accessed on 2016-06-25)
	 * 
	 * @param file
	 *            the csv file to parse
	 * @param zoneId
	 *            sets the time zone id of the {@code WeatherData} object to
	 *            {@code zoneId}
	 * 
	 * @return the content of the file as {@code WeatherData} object
	 * 
	 * @throws IOException
	 *             if an error occurred while reading {@code file}
	 * @throws NumberFormatException
	 *             if a number cannot be parsed
	 * @throws DateTimeParseException
	 *             if a date cannot be parsed
	 */
	public WeatherData parse(File file, ZoneId zoneId) throws IOException {
		return new WeatherData(parseCsvFile(file), zoneId);
	}

	/**
	 * Helper method to parse the file csv file provided as {@code file}. The
	 * returned {@code TreeMap} can be passed to the {@code WeatherData}
	 * constructor.
	 * 
	 * @param file
	 *            the file to parse
	 * @return a {@code TreeMap} representation of the parsed file
	 * 
	 * @throws IOException
	 *             if an error occurred while reading {@code file}
	 * @throws NumberFormatException
	 *             if a number cannot be parsed
	 * @throws DateTimeParseException
	 *             if a date cannot be parsed
	 */
	private static TreeMap<LocalDateTime, WeatherRecord> parseCsvFile(File file)
			throws IOException {

		logger.info("Parsing " + file.getAbsolutePath() + "...");

		CSVFormat csvFormat = CSVFormat.DEFAULT.withDelimiter(DELIMITER)
				.withHeader().withIgnoreSurroundingSpaces(true);

		CSVParser csvParser = CSVParser.parse(file, Charset.forName("UTF-8"),
				csvFormat);

		TreeMap<LocalDateTime, WeatherRecord> ret = new TreeMap<>();

		int noRecords = 0;
		for (CSVRecord csvRecord : csvParser) {
			noRecords++;
			// Skip the record if it does not contain the required columns.
			if (csvRecord.isSet(DATE_COL) && csvRecord.isSet(TEMPERATURE_COL)
					&& csvRecord.isSet(RELATIVE_HUMIDITY_COL)) {

				String dateStr = csvRecord.get(DATE_COL);
				String tempStr = csvRecord.get(TEMPERATURE_COL);
				String humidityStr = csvRecord.get(RELATIVE_HUMIDITY_COL);

				LocalDateTime time = LocalDateTime.parse(dateStr,
						DateTimeFormatter.ofPattern(DATE_FORMAT));
				double temp = Double.parseDouble(tempStr);
				double humidity = Double.parseDouble(humidityStr);

				ret.put(time, new WeatherRecord(time, temp, humidity));

			} else {
				logger.debug("skipped record: " + csvRecord);
			}
		}
		if (noRecords != ret.size()) {
			logger.warn((noRecords - ret.size())
					+ " record(s) skipped while parsing");
		}
		logger.info("done (time range: " + ret.firstKey() + " - "
				+ ret.lastKey() + ")");
		return ret;
	}

}
