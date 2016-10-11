package joachimrussig.heatstressrouting.weatherdata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

/**
 * Helper class to download the latest weather data from the DWD server and to
 * update the existing file.
 * 
 * @author Joachim Rußig
 *
 */
public class WeatherDataUpdater {

	private static Logger logger = LoggerFactory
			.getLogger(WeatherDataUpdater.class);

	private WeatherDataUpdater() {
	}

	/**
	 * Downloads and (optionally) updates the existing weather data file.
	 * 
	 * @param weatherDataFile
	 *            the weather data file to be updated
	 * @param zipFileUrl
	 *            URL from which were the new data should be downloaded form
	 * @param updateFile
	 *            should the existing file be updated?
	 * @param backupOldFile
	 *            should the existing file be backed up?
	 * @return the weather data downloaded from the web server
	 * @throws IOException
	 */
	public static Optional<WeatherData> updateWeatherData(File weatherDataFile,
			URL zipFileUrl, boolean updateFile, boolean backupOldFile)
			throws IOException {

		Optional<File> weatherDataNew = downloadWeatherDataFile(zipFileUrl);
		logger.debug("weatherDataNew = " + weatherDataNew);

		if (weatherDataNew.isPresent()) {

			if (updateFile) {
				logger.debug("Updating " + weatherDataFile.getCanonicalPath()
						+ "...");

				if (backupOldFile) {
					File weatherDataBak = new File(weatherDataFile + ".bak");
					logger.debug("Backuping existing file to "
							+ weatherDataBak.getCanonicalPath());
					Files.copy(weatherDataFile, weatherDataBak);
				}

				logger.debug("Copying new file "
						+ weatherDataNew.get().getCanonicalPath());
				Files.copy(weatherDataNew.get(), weatherDataFile);
				logger.debug("done");
			}

			return Optional
					.of(new WeatherDataParser().parse(weatherDataNew.get()));

		}

		return Optional.empty();
	}

	/**
	 * Downloads the weather data from the specified URL, unzip the downloaded
	 * archive and returns a file representing the downloaded and unpacked file.
	 * 
	 * @param zipFileUrl
	 *            the URL from which the data should be downloaded
	 * @return a {@link File} representing the downloaded file
	 * @throws IOException
	 */
	private static Optional<File> downloadWeatherDataFile(URL zipFileUrl)
			throws IOException {

		String zipFilePath = zipFileUrl.getPath();
		String zipFileName = zipFilePath
				.substring(zipFilePath.lastIndexOf('/'));

		File zipFile = File.createTempFile(zipFileName + "_", ".zip");
		String outputDirName = zipFile.getCanonicalPath();
		if (outputDirName.contains("."))
			outputDirName = outputDirName.substring(0,
					outputDirName.lastIndexOf('.'));
		File outputDir = new File(outputDirName);

		if (!outputDir.exists()) {
			outputDir.mkdir();
		}

		logger.debug("Download zip file from " + zipFileUrl + "...");
		URLConnection urlConnection = zipFileUrl.openConnection();
		InputStream input = urlConnection.getInputStream();

		byte[] buffer = new byte[4096];
		int n = -1;

		OutputStream output = new FileOutputStream(zipFile);
		while ((n = input.read(buffer)) != -1) {
			output.write(buffer, 0, n);
		}
		output.close();

		logger.debug("done (path = " + zipFile.getCanonicalPath() + ")");

		unZip(zipFile, outputDir);

		Optional<File> weatherDataNew = Arrays.stream(outputDir.listFiles())
				.filter(File::isFile)
				.filter(f -> f.getName().startsWith("produkt_temp_Terminwerte"))
				.findAny();

		// Clean up the created files and directories
		zipFile.delete();
		outputDir.deleteOnExit();

		return weatherDataNew;
	}

	/**
	 * Unzips the file in the zip file {@code zipFile} to the directory
	 * specified in {@code outputDir}.
	 * <p>
	 * 
	 * Based on
	 * https://www.mkyong.com/java/how-to-decompress-files-from-a-zip-file/
	 * 
	 * @param zipFile
	 *            the zip file to unzip
	 * @param outputDir
	 *            the directory were the files should be stored
	 * @throws IOException
	 */
	private static void unZip(File zipFile, File outputDir) throws IOException {

		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}

		ZipInputStream zipInputStream = new ZipInputStream(
				new FileInputStream(zipFile));

		byte[] buffer = new byte[1024];

		logger.debug("unzipping " + zipFile.getCanonicalPath() + "...");

		ZipEntry zipEntry;
		while ((zipEntry = zipInputStream.getNextEntry()) != null) {
			String fileName = zipEntry.getName();
			File newFile = new File(outputDir + File.separator + fileName);

			// create all non exists folders
			// else you will hit FileNotFoundException for compressed folder
			new File(newFile.getParent()).mkdirs();

			logger.debug("unzip file: " + newFile.getAbsoluteFile());

			FileOutputStream fileOutputStream = new FileOutputStream(newFile);

			int len;
			while ((len = zipInputStream.read(buffer)) > 0) {
				fileOutputStream.write(buffer, 0, len);
			}

			fileOutputStream.close();
		}
		zipInputStream.closeEntry();
		zipInputStream.close();

		logger.debug("done");

	}

}
