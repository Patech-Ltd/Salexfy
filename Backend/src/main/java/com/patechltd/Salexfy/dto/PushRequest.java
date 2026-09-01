package com.patechltd.Salexfy.dto;

import java.util.List;

public record PushRequest(String deviceId, List<ChangeDto> changes) {
}
