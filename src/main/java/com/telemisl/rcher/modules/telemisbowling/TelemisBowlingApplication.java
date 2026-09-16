package com.telemisl.rcher.modules.telemisbowling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TelemisBowlingApplication {

    static void main(String[] args) {
        SpringApplication.run(TelemisBowlingApplication.class, args);
    }

}
