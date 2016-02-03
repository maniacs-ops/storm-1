package org.apache.storm.statistics.reporters;

import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import org.apache.storm.Config;
import org.apache.storm.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CsvPreparableReporter implements PreparableReporter<CsvReporter> {
    private final static Logger LOG = LoggerFactory.getLogger(CsvPreparableReporter.class);
    CsvReporter reporter = null;

    @Override
    public void prepare(MetricRegistry metricsRegistry, Map stormConf) {
        LOG.info("Preparing...");
        CsvReporter.Builder builder = CsvReporter.forRegistry(metricsRegistry);

        Locale locale = (Locale) stormConf.get(":locale");
        if (locale != null) {
            builder.formatFor(locale);
        }
        String rateUnit = Utils.getString(stormConf.get(":rate-unit"), null);
        if (rateUnit != null) {
            builder.convertRatesTo(TimeUnit.valueOf(rateUnit));
        }
        String durationUnit = Utils.getString(stormConf.get(":duration-unit"), null);
        if (durationUnit != null) {
            builder.convertDurationsTo(TimeUnit.valueOf(durationUnit));
        }
        MetricFilter filter = (MetricFilter) stormConf.get(":filter");
        if (filter != null) {
            builder.filter(filter);
        }
        String localStormDirLocation = Utils.getString(stormConf.get(Config.STORM_LOCAL_DIR), ".");
        File logDir = new File(localStormDirLocation + "csvmetrics" );
        validateCreateOutputDir(logDir);
        reporter = builder.build(logDir);
    }

    @Override
    public void start() {
        if (reporter != null) {
            LOG.info("Starting...");
            reporter.start(10, TimeUnit.SECONDS);
        } else {
            throw new IllegalStateException("Attempt to start without preparing " + getClass().getSimpleName());
        }
    }

    @Override
    public void stop() {
        if (reporter != null) {
            LOG.info("Stopping...");
            reporter.stop();
        } else {
            throw new IllegalStateException("Attempt to stop without preparing " + getClass().getSimpleName());
        }
    }


    private void validateCreateOutputDir(File dir) {
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if (!dir.canWrite()) {
            throw new IllegalStateException(dir.getName() + " does not have write permissions.");
        }
        if (!dir.isDirectory()) {
            throw new IllegalStateException(dir.getName() + " is not a directory.");
        }
    }
}
