package org.bujian.self;

import com.feiniaojin.gracefulresponse.EnableGracefulResponse;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableGracefulResponse
@SpringBootApplication
public class BujianApplication {
    public static void main(String[] args) {
        SpringApplication.run(BujianApplication.class, args);
    }
}
