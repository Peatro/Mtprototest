package com.peatroxd.mtprototest.checker.service;

import java.util.Optional;

public interface ProxyCheckCycleTrackingService {
    void markStarted(String trigger);
    void markFinished(String trigger, int totalChecked, int archivedCount);
    void markFailed(String trigger, String message);
    void markSkipped(String trigger, String message);
    Optional<CheckCycleSnapshot> latestSuccessfulCycle();
    Optional<CheckCycleSnapshot> latestCycle();
}
