package com.peatroxd.mtprototest.proxy.ranking.segment;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SessionDiagnosticRequest(
        @NotNull @Min(1) @Max(100) Integer failedAttempts,
        @Size(max = 200) String isp,
        @Size(max = 500) String notes
) {}
