package com.ai.e2e.tx.example.service;

import com.nortal.healthcare.test.BehaviourRunner;
import io.cucumber.junit.CucumberOptions;

@CucumberOptions(extraGlue = { "com.nortal.healthcare.service.glue" })
public class GatewayBehaviourTest extends BehaviourRunner {

}
