package com.oncall.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAccumulator;

@Component
public class KnowledgeMetrics {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeMetrics.class);
    private final AtomicLong totalSearches = new AtomicLong(0);
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong totalLatencyMs = new AtomicLong(0);
    private final DoubleAccumulator maxBm25Score = new DoubleAccumulator(Double::max, 0.0);

    public void recordSearch(boolean hit, long latencyMs, double topBm25Score) {
        totalSearches.incrementAndGet();
        if (hit) hitCount.incrementAndGet();
        else missCount.incrementAndGet();
        totalLatencyMs.addAndGet(latencyMs);
        maxBm25Score.accumulate(topBm25Score);
    }

    public double getHitRate() {
        long total = totalSearches.get();
        return total == 0 ? 0.0 : (double) hitCount.get() / total;
    }

    public double getAvgLatencyMs() {
        long total = totalSearches.get();
        return total == 0 ? 0.0 : (double) totalLatencyMs.get() / total;
    }

    public double getMaxBm25Score() { return maxBm25Score.get(); }
    public long getTotalSearches() { return totalSearches.get(); }
    public long getHitCount() { return hitCount.get(); }
    public long getMissCount() { return missCount.get(); }

    public String formatSearchMetrics(int hitDocs, double bm25MaxScore, long latencyMs) {
        return String.format(
            "本\u6b21\u68c0\u7d22\uff1a\u547d\u4e2d\u6587\u6863 %d \u7bc7\uff0cBM25\u6700\u9ad8\u5f97\u5206\uff1a%.2f\uff0c\u68c0\u7d22\u8017\u65f6\uff1a%dms",
            hitDocs, bm25MaxScore, latencyMs
        );
    }
}
