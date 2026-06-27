package com.rsh.fcl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rsh.fcl.exception.CricketerExistsException;
import com.rsh.fcl.exception.CricketerNotFoundException;
import com.rsh.fcl.model.Cricketer;
import com.rsh.fcl.model.CricketerType;
import com.rsh.fcl.repository.CricketerRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CricketerServiceTest {

  @Mock
  private CricketerRepository cricketerRepository;

  private CricketerService cricketerService;

  @BeforeEach
  void setUp() {
    cricketerService = new CricketerService(cricketerRepository);
  }

  @Test
  void createCricketerPersistsValidId() {
    when(cricketerRepository.existsById("abc_xyz")).thenReturn(false);
    when(cricketerRepository.save(any(Cricketer.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Cricketer cricketer = cricketerService.createCricketer("abc_xyz", "Sachin",
        CricketerType.BATTER);

    assertThat(cricketer.getGlobalUniqueId()).isEqualTo("abc_xyz");
    assertThat(cricketer.getName()).isEqualTo("Sachin");
    verify(cricketerRepository).save(cricketer);
  }

  @Test
  void createCricketerRejectsBadFormat() {
    assertThatThrownBy(() -> cricketerService.createCricketer("abcd_xy", "X", CricketerType.BATTER))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("format");
    assertThatThrownBy(() -> cricketerService.createCricketer("abcxyz", "X", CricketerType.BATTER))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> cricketerService.createCricketer("ab1_xyz", "X", CricketerType.BATTER))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void createCricketerRejectsDuplicate() {
    when(cricketerRepository.existsById("abc_xyz")).thenReturn(true);
    assertThatThrownBy(() -> cricketerService.createCricketer("abc_xyz", "X", CricketerType.BATTER))
        .isInstanceOf(CricketerExistsException.class);
  }

  @Test
  void getCricketerThrowsWhenMissing() {
    when(cricketerRepository.findById("non_exi")).thenReturn(Optional.empty());
    assertThatThrownBy(() -> cricketerService.getCricketer("non_exi"))
        .isInstanceOf(CricketerNotFoundException.class);
  }

  @Test
  void updateCricketerChangesNameAndType() {
    Cricketer existing = new Cricketer("abc_xyz", "Old", CricketerType.BATTER);
    when(cricketerRepository.findById("abc_xyz")).thenReturn(Optional.of(existing));
    when(cricketerRepository.save(any(Cricketer.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Cricketer updated = cricketerService.updateCricketer("abc_xyz", "New", CricketerType.BOWLER);

    assertThat(updated.getName()).isEqualTo("New");
    assertThat(updated.getType()).isEqualTo(CricketerType.BOWLER);
  }

  @Test
  void deleteCricketerDelegates() {
    Cricketer existing = new Cricketer("abc_xyz", "X", CricketerType.BATTER);
    when(cricketerRepository.findById("abc_xyz")).thenReturn(Optional.of(existing));

    cricketerService.deleteCricketer("abc_xyz");

    verify(cricketerRepository).delete(existing);
  }
}
