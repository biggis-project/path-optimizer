package joachimrussig.heatstressrouting.webapi;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Context;

import org.glassfish.jersey.server.ResourceConfig;
import org.quartz.DateBuilder;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.ScheduleBuilder;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import joachimrussig.heatstressrouting.webapi.util.UpdateWeatherData;

/**
 * A simple REST-API for the routing and optimal time service.
 * 
 * @author Joachim Rußig
 *
 */
@ApplicationPath("api")
public class WebApi extends ResourceConfig {

	private Logger logger = LoggerFactory.getLogger(WebApi.class);

	public WebApi(@Context ServletContext servletContext) {

		System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY,
				"INFO");

		ResourceBinder resourceBinder = new ResourceBinder();

		register(resourceBinder);
		packages(true, "joachimrussig.heatstressrouting");

		// Scheduler a task to periodically update the weather data file from
		// the DWD FTP Server
		try {

			int interval = 12;
			TimeUnit unit = TimeUnit.HOUR;
			int startDelay = 60;

			Scheduler scheduler = createUpdateWeatherDataScheduler(interval,
					unit, resourceBinder, startDelay);
			scheduler.start();

			logger.info("Scheulder weahter data update (time interval: "
					+ interval + " " + unit.toString().toLowerCase() + ")");

		} catch (SchedulerException | URISyntaxException | IOException e) {
			// TODO Improve error handling
			throw new RuntimeException(e);
		}

	}

	private enum TimeUnit {
		HOUR, MINUTE, SECOND
    }

	private Scheduler createUpdateWeatherDataScheduler(int interval,
			TimeUnit unit, ResourceBinder resourceBinder, int startDelay)
			throws SchedulerException, IOException, URISyntaxException {

		StdSchedulerFactory schedulerFactory = new StdSchedulerFactory();
		Properties properties = new Properties();
		properties.setProperty("org.quartz.threadPool.threadCount",
				String.valueOf(1));
		schedulerFactory.initialize(properties);

		Scheduler scheduler = schedulerFactory.getScheduler();

		JobDetail job = JobBuilder.newJob(UpdateWeatherData.class).build();

		job.getJobDataMap().put("baseDir", ResourceBinder
				.getDataDir(this.getClass()).toFile().getCanonicalPath());
		job.getJobDataMap().put("resourceBinder", resourceBinder);

		int secs = 60;
		switch (unit) {
		case HOUR:
			secs = interval * 60 * 60;
			break;
		case MINUTE:
			secs = interval * 60;
		case SECOND:
			secs = interval;
			break;
		default:
			break;
		}

		ScheduleBuilder<SimpleTrigger> scheduleBuilder = SimpleScheduleBuilder
				.simpleSchedule().withIntervalInSeconds(secs).repeatForever();

		TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger()
				.withIdentity("trigger1", "group1");

		if (startDelay < 1)
			triggerBuilder.startNow();
		else
			triggerBuilder.startAt(
					DateBuilder.futureDate(startDelay, IntervalUnit.SECOND));

		Trigger trigger = triggerBuilder.withSchedule(scheduleBuilder).build();

		scheduler.scheduleJob(job, trigger);

		return scheduler;

	}
}
