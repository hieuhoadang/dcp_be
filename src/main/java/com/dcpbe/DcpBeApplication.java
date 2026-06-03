package com.dcpbe;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DcpBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(DcpBeApplication.class, args);
    }

}
