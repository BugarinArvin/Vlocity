package com.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.Transactional;

import com.scheduler.service.SchedulerServiceImpl;

@SpringBootApplication
public class SchedulerApplication implements CommandLineRunner {

	@Autowired
	private SchedulerServiceImpl schedulerService;

	// java -jar target/scheduler-0.0.1-SNAPSHOT.jar
	public static void main(String[] args) {
		  SpringApplication app = new SpringApplication(SchedulerApplication.class);
	       // app.setBannerMode(Banner.Mode.OFF);
	        app.run(args);
	}
	
	@Transactional
	public void run(String... args) {
		schedulerService.startScheduler();
	}
}
