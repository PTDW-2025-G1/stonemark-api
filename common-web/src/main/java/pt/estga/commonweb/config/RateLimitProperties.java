package pt.estga.commonweb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties("rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private List<String> excludePaths = List.of("/actuator", "/actuator/**");
    private BandwidthConfig defaultLimit = new BandwidthConfig();
    private Map<String, BandwidthConfig> paths = new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getExcludePaths() {
        return excludePaths;
    }

    public void setExcludePaths(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }

    public BandwidthConfig getDefaultLimit() {
        return defaultLimit;
    }

    public void setDefaultLimit(BandwidthConfig defaultLimit) {
        this.defaultLimit = defaultLimit;
    }

    public Map<String, BandwidthConfig> getPaths() {
        return paths;
    }

    public void setPaths(Map<String, BandwidthConfig> paths) {
        this.paths = paths;
    }

    public static class BandwidthConfig {
        private long capacity = 100;
        private long refillTokens = 100;
        private long refillPeriod = 1;
        private ChronoUnit refillUnit = ChronoUnit.MINUTES;

        public long getCapacity() {
            return capacity;
        }

        public void setCapacity(long capacity) {
            this.capacity = capacity;
        }

        public long getRefillTokens() {
            return refillTokens;
        }

        public void setRefillTokens(long refillTokens) {
            this.refillTokens = refillTokens;
        }

        public long getRefillPeriod() {
            return refillPeriod;
        }

        public void setRefillPeriod(long refillPeriod) {
            this.refillPeriod = refillPeriod;
        }

        public ChronoUnit getRefillUnit() {
            return refillUnit;
        }

        public void setRefillUnit(ChronoUnit refillUnit) {
            this.refillUnit = refillUnit;
        }
    }
}
