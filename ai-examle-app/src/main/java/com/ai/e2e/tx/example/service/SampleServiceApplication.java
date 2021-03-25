package com.ai.e2e.tx.example.service;

import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Configuration
@SpringBootApplication
public class SampleServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(SampleServiceApplication.class, args);
  }

}

@RestController
class ExampleController {

  @PostMapping("/example")
  public String testPost(HttpServletRequest request, @RequestBody String body) {
    return "response";
  }

  @GetMapping("/example")
  public String testGet(HttpServletRequest request) {
    return "response";
  }

}
