package com.rsh.fcl.dto;

import com.rsh.fcl.model.CricketerType;

public record CricketerResponse(String globalUniqueId, String name, CricketerType type) {
}
