package com.rsh.fcl.controller;

import com.rsh.fcl.dto.CricketerRequest;
import com.rsh.fcl.dto.CricketerResponse;
import com.rsh.fcl.mapper.DtoMapper;
import com.rsh.fcl.service.CricketerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cricketers")
@Validated
public class CricketerController {

  private final CricketerService cricketerService;

  public CricketerController(CricketerService cricketerService) {
    this.cricketerService = cricketerService;
  }

  @PostMapping
  public ResponseEntity<CricketerResponse> createCricketer(
      @RequestParam @NotBlank String globalUniqueId,
      @Valid @RequestBody CricketerRequest request) {
    CricketerResponse response = DtoMapper.toCricketerResponse(
        cricketerService.createCricketer(globalUniqueId, request.name(), request.type()));
    return ResponseEntity.created(URI.create("/api/cricketers/" + response.globalUniqueId()))
        .body(response);
  }

  @GetMapping
  public Page<CricketerResponse> getCricketers(
      @PageableDefault(size = 20, sort = "globalUniqueId") Pageable pageable) {
    return cricketerService.getCricketers(pageable).map(DtoMapper::toCricketerResponse);
  }

  @GetMapping("/{globalUniqueId}")
  public CricketerResponse getCricketer(@PathVariable String globalUniqueId) {
    return DtoMapper.toCricketerResponse(cricketerService.getCricketer(globalUniqueId));
  }

  @PutMapping("/{globalUniqueId}")
  public CricketerResponse updateCricketer(
      @PathVariable String globalUniqueId,
      @Valid @RequestBody CricketerRequest request) {
    return DtoMapper.toCricketerResponse(
        cricketerService.updateCricketer(globalUniqueId, request.name(), request.type()));
  }

  @DeleteMapping("/{globalUniqueId}")
  public ResponseEntity<Void> deleteCricketer(@PathVariable String globalUniqueId) {
    cricketerService.deleteCricketer(globalUniqueId);
    return ResponseEntity.noContent().build();
  }
}
