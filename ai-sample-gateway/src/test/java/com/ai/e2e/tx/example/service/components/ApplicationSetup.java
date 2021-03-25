package com.ai.e2e.tx.example.service.components;

import com.nortal.healthcare.test.images.builder.EmrImageFromDockerfileBuilder;
import com.nortal.healthcare.test.images.builder.ReusableImageFromDockerfile;
import com.nortal.healthcare.test.services.AbstractApplicationSetup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ApplicationSetup extends AbstractApplicationSetup {

  @Autowired
  private EmrImageFromDockerfileBuilder dockerfileBuilder;

  @Override
  protected ReusableImageFromDockerfile getImage() {
    return dockerfileBuilder
        .name("gateway")
        .debugPort(getDebugPort())
        .jacocoPort(getJacocoPort())
        .memory("256m")
        .build();
  }

}
