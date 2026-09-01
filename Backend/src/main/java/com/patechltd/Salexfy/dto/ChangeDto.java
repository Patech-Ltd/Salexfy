package com.patechltd.Salexfy.dto;

public record ChangeDto(long seq, String entityType, String recordId, String operation,
                        String payload, long updatedAt) {
}
