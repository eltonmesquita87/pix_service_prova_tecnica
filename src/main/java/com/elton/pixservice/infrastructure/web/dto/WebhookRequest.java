package com.elton.pixservice.infrastructure.web.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebhookRequest {
    @NotBlank(message = "End to end ID is required")
    private String endToEndId;

    @NotBlank(message = "Event ID is required")
    private String eventId;

    @NotBlank(message = "Event type is required")
    private String eventType;

    @NotNull(message = "Occurred at is required")
    private LocalDateTime occurredAt;
}
