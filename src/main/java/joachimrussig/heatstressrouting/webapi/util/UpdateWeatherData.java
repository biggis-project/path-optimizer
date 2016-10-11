package joachimrussig.heatstressrouting.webapi.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import joachimrussig.heatstressrouting.HeatStressRouting;
import joachimrussig.heatstressrouting.weatherdata.WeatherData;
import joachimrussig.heatstressrouting.weatherdata.WeatherDataParser;
import joachimrussig.heatstressrouting.webapi.ResourceBinder;

/**
 * A schedulered job to automatically update the {@link WeatherData} used e.g.
 * for routing from the sever of the Deutscher Wetterdienst (DWD). For a short
 * discription of the weather data see here {@link WeatherDataParser}. The data
 * are fetched from the URL specified in
 * {@link UpdateWeatherData#WATHER_DATA_ZIP_URL}.
 * 
 * @author Joachim Rußig
 *
 */
public class UpdateWeatherData implements org.quartz.Job {

	private static Logger logger = LoggerFactory
			.getLogger(UpdateWeatherData.class);
	/**
	 * See DWD station id of the weather station in Rheinstetten.
	 */
	public static final String WEATHER_STAION_ID = "04177";
	/**
	 * The URL from which the weather data are downloaded.
	 */
	public static final String WATHER_DATA_ZIP_URL = "ftp://ftp-cdc.dwd.de/pub/CDC/"
			+ "observations_germany/climate/hourly/air_temperature/recent/"
			+ "stundenwerte_TU_04177_akt.zip";

	@Override
	public void execute(JobExecutionContext context)
			throws JobExecutionException {
		String baseDir = context.getJobDetail().getJobDataMap()
				.getString("baseDir");
		ResourceBinder resourceBinder = (ResourceBinder) context.getJobDetail()
				.getJobDataMap().get("resourceBinder");

		logger.info("Updateing weather data (baseDir = " + baseDir + ")");

		try {

			boolean updated = false;
			if (resourceBinder.getRoutingHelper() != null) {
				File weatherData = Paths
						.get(baseDir, HeatStressRouting.WEATHER_FILE_NAME)
						.toFile();
				resourceBinder.getRoutingHelper().updateWeatherData(weatherData,
						new URL(WATHER_DATA_ZIP_URL), true, true);
				updated = true;
			}

			if (updated)
				logger.info("file updated");
			else
				logger.info("file not updated");
		} catch (IOException e) {
			throw new JobExecutionException(e);
		}

	}

}
