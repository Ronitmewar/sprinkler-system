package com.sprinklersystem.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.sprinklersystem.SprinklerSystemApplication;

@Service
public class SprinklerRestartAppService {

	private static Logger log = LoggerFactory.getLogger(SprinklerRestartAppService.class);

	@Async
	public void asyncApplicationRestart() {
		try {
			log.info("Restarting App");
			SprinklerSystemApplication.restart();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
