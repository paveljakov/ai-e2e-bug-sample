package com.ai.e2e.tx.example.service.glue;

import com.ai.e2e.tx.example.service.ServiceTestConfig;
import com.nortal.healthcare.test.glue.ContextSteps;
import io.cucumber.java.en.Given;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = ServiceTestConfig.class)
public class ContextStep extends ContextSteps {

  @Given("no-op step")
  public void noOpStep() {
    //This step here is so that Cucumber would pick up the ContextConfiguration annotation.
    //As it must be only specified on a single step class
  }

}
