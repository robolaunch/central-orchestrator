package org.robolaunch.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;
import org.robolaunch.model.account.Organization;
import org.robolaunch.repository.abstracts.KogitoRepository;

import io.quarkus.arc.log.LoggerName;

@ApplicationScoped
public class KogitoService {
  @Inject
  Logger log;

  @Inject
  KogitoService kogitoService;

  @Inject
  KogitoRepository kogitoRepository;

  @LoggerName("kogitoService")
  Logger kogitoLogger;

  public String getProcessId(Organization organization, String teamId) {
    try {
      return kogitoRepository.getProcessId(organization, teamId);
    } catch (Exception e) {
      // TODO: handle exception
      return null;
    }
  }
}
