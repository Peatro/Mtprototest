package com.peatroxd.mtprototest.proxy.ranking.segment;

import jakarta.validation.constraints.NotNull;

public record SignalRequest(@NotNull SignalEvent event) {}
