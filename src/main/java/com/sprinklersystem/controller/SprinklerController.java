package com.sprinklersystem.controller;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.time.LocalTime;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.sprinklersystem.SprinklerSystemApplication;
import com.sprinklersystem.service.SprinklerService;

@RestController
@RequestMapping("/sprinkler")
public class SprinklerController {

	@Autowired
	private SprinklerService sprinklerService;

	private static final Logger LOG = LoggerFactory.getLogger(SprinklerController.class);

	private static GpioController gpio = GpioFactory.getInstance();
	private static GpioPinDigitalOutput pin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_15, "Water Motor",
			PinState.LOW);

	private static final String MOTOR_STARTED_MSG = "Water motor started!!";
	private static final String MOTOR_STOPPED_MSG = "Water motor stopped!!";
	@Value("${scheduler.cron.expression}")
	private String schedulerCronExp;

	@GetMapping("/")
	public String healthCheck() {
		LOG.info("I am alive!!");
		return "I am alive!!";
	}

	@GetMapping("/restart")
	public void restart() {
		LOG.info("Restarting sprinkler system!!");
		SprinklerSystemApplication.restart();
	}

	@GetMapping("/on")
	public String turnOn() {
		try {
			pin.setState(PinState.HIGH);
			LOG.info(MOTOR_STARTED_MSG);
			return MOTOR_STARTED_MSG;
		} catch (Exception e) {
			e.printStackTrace();
			return convertStacktraceToString(e);
		}

	}

	@GetMapping("/off")
	public String turnOff() {
		try {
			pin.setState(PinState.LOW);
			LOG.info(MOTOR_STOPPED_MSG);
			return MOTOR_STOPPED_MSG;
		} catch (Exception e) {
			e.printStackTrace();
			return convertStacktraceToString(e);
		}
	}

	@GetMapping("/timer/{time}")
	public String specificTimePeriod(@PathVariable String time) {
		try {
			pin.setState(PinState.HIGH);
			LOG.info(MOTOR_STARTED_MSG);

			Thread.sleep(Integer.parseInt(time) * 1000L);

			pin.setState(PinState.LOW);
			LOG.info(MOTOR_STOPPED_MSG);

			LOG.info("Water motor ran for {} seconds!!", time);
			return String.format("Water motor ran for %s seconds!!", time);
		} catch (Exception e) {
			e.printStackTrace();
			return convertStacktraceToString(e);
		}

	}

	@GetMapping("/scheduler/setTime/{hour}/{minute}")
	public String setScheduler(@PathVariable String hour, @PathVariable String minute) {
		try {
			String cronExp = String.format("0 %s %s * * ?", minute, hour);
			updateSchedulerTime(cronExp);
			LOG.info("Scheduler is set for {}:{} everyday", hour, minute);
			sprinklerService.asyncApplicationRestart();
			return String.format("Scheduler is set for %s:%s everyday", hour, minute);
		} catch (Exception e) {
			e.printStackTrace();
			return convertStacktraceToString(e);
		}

	}

	@GetMapping("/scheduler/getTime")
	public String getSchedulerTime() {
		try {
			int hourValue = Integer.parseInt(schedulerCronExp.split("\\s+")[2]);
			String timeMeridiem = (hourValue >= 12) ? "PM" : "AM";
			String hour = new DecimalFormat("00").format((hourValue + 11) % 12 + 1L);
			String minute = new DecimalFormat("00").format(Integer.parseInt(schedulerCronExp.split("\\s+")[1]));
			LOG.info("Scheduler time is {}:{} {} everyday", hour, minute, timeMeridiem);
			return String.format("Scheduler time is %s:%s %s everyday", hour, minute, timeMeridiem);
		} catch (Exception e) {
			e.printStackTrace();
			return convertStacktraceToString(e);
		}

	}

	private String convertStacktraceToString(Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}

	private void updateSchedulerTime(String cronExp) throws Exception {
		try (InputStream in = new FileInputStream("./application.properties");
				OutputStream out = new FileOutputStream("./application.properties")) {
			Properties props = new Properties();
			props.load(in);

			props.setProperty("scheduler.cron.expression", cronExp);
			props.store(out, null);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	@Scheduled(cron = "${scheduler.cron.expression}")
	public void sprinklerScheduler() {
		try {
			int time = 5;
			pin.setState(PinState.HIGH);
			LOG.info(MOTOR_STARTED_MSG + LocalTime.now());

			Thread.sleep(time * 1000L);

			pin.setState(PinState.LOW);
			LOG.info(MOTOR_STOPPED_MSG + LocalTime.now());

			LOG.info("Water motor ran for {} seconds!!", time);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
