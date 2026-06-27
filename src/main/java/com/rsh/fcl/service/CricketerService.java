package com.rsh.fcl.service;

import com.rsh.fcl.exception.CricketerExistsException;
import com.rsh.fcl.exception.CricketerNotFoundException;
import com.rsh.fcl.model.Cricketer;
import com.rsh.fcl.model.CricketerType;
import com.rsh.fcl.repository.CricketerRepository;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CricketerService {

  private static final Pattern ID_PATTERN = Pattern.compile("^[a-zA-Z]{3}_[a-zA-Z]{3}$");

  private final CricketerRepository cricketerRepository;

  public CricketerService(CricketerRepository cricketerRepository) {
    this.cricketerRepository = cricketerRepository;
  }

  @Transactional
  public Cricketer createCricketer(String globalUniqueId, String name, CricketerType type) {
    validateId(globalUniqueId);
    if (cricketerRepository.existsById(globalUniqueId)) {
      throw new CricketerExistsException(globalUniqueId);
    }
    return cricketerRepository.save(new Cricketer(globalUniqueId, name, type));
  }

  @Transactional(readOnly = true)
  public Page<Cricketer> getCricketers(Pageable pageable) {
    return cricketerRepository.findAll(pageable);
  }

  @Transactional(readOnly = true)
  public Cricketer getCricketer(String globalUniqueId) {
    return findCricketer(globalUniqueId);
  }

  @Transactional
  public Cricketer updateCricketer(String globalUniqueId, String name, CricketerType type) {
    Cricketer cricketer = findCricketer(globalUniqueId);
    cricketer.setName(name);
    cricketer.setType(type);
    return cricketerRepository.save(cricketer);
  }

  @Transactional
  public void deleteCricketer(String globalUniqueId) {
    Cricketer cricketer = findCricketer(globalUniqueId);
    cricketerRepository.delete(cricketer);
  }

  private Cricketer findCricketer(String globalUniqueId) {
    return cricketerRepository.findById(globalUniqueId)
        .orElseThrow(() -> new CricketerNotFoundException(globalUniqueId));
  }

  private static void validateId(String globalUniqueId) {
    if (globalUniqueId == null || !ID_PATTERN.matcher(globalUniqueId).matches()) {
      throw new IllegalArgumentException(
          "Cricketer global unique id must match format <3 letters>_<3 letters>");
    }
  }
}
