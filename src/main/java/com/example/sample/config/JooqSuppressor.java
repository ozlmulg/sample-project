package com.example.sample.config;

import io.micronaut.context.annotation.Context;
import jakarta.inject.Singleton;
import javax.annotation.PostConstruct;

@Singleton
@Context
public class JooqSuppressor {

  @PostConstruct
  public void init() {
    System.setProperty("org.jooq.no-tips", "true");
    System.setProperty("org.jooq.no-logo", "true");
  }
}
