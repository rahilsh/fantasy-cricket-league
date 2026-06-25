package com.rsh.fcl.config;

import java.util.Arrays;
import java.util.stream.Stream;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseMigrationConfiguration {

  @Bean
  SpringLiquibase liquibase(DataSource dataSource) {
    SpringLiquibase liquibase = new SpringLiquibase();
    liquibase.setDataSource(dataSource);
    liquibase.setChangeLog("classpath:db/changelog/db.changelog-master.yaml");
    liquibase.setDatabaseChangeLogTable("databasechangelog");
    liquibase.setDatabaseChangeLogLockTable("databasechangeloglock");
    return liquibase;
  }

  @Bean
  static BeanFactoryPostProcessor entityManagerFactoryDependsOnLiquibase() {
    return (ConfigurableListableBeanFactory beanFactory) -> {
      if (!beanFactory.containsBeanDefinition("entityManagerFactory")) {
        return;
      }
      BeanDefinition entityManagerFactory = beanFactory.getBeanDefinition("entityManagerFactory");
      String[] dependsOn = entityManagerFactory.getDependsOn();
      String[] updatedDependsOn = Stream.concat(
              dependsOn == null ? Stream.empty() : Arrays.stream(dependsOn),
              Stream.of("liquibase"))
          .distinct()
          .toArray(String[]::new);
      entityManagerFactory.setDependsOn(updatedDependsOn);
    };
  }
}
