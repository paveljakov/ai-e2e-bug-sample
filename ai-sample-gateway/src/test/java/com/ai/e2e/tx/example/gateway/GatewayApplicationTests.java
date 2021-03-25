package com.ai.e2e.tx.example.gateway;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = { "tests=true", "spring.main.web-application-type=reactive" })
public class GatewayApplicationTests {

  @Test
  public void contextLoads() {
  }

}
