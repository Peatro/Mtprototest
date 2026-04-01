package com.peatroxd.mtprototest.scoring.service;

public interface ProxyScoringService {
    int calculateScore(boolean alive, long latencyMs);
}
