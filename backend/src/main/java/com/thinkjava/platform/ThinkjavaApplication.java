package com.thinkjava.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
@RestController
@RequestMapping("/api")
public class ThinkjavaApplication {

    // allow the Angular dev origin
    @CrossOrigin(origins = "http://localhost:4200")
    @GetMapping("/ping")
    public String ping() {
        return "Hello, ThinkJava!";
    }

    public static void main(String[] args) {
        SpringApplication.run(ThinkjavaApplication.class, args);
    }
}
