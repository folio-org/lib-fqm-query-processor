package org.folio.fql.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "org.folio.fql.service")
public class FqlConfiguration {}
