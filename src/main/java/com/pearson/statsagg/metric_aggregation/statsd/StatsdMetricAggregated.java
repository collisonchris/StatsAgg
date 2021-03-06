package com.pearson.statsagg.metric_aggregation.statsd;

import java.math.BigDecimal;
import com.pearson.statsagg.metric_aggregation.GenericMetricFormat;
import com.pearson.statsagg.metric_aggregation.GraphiteMetricFormat;
import com.pearson.statsagg.metric_aggregation.OpenTsdbMetricFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeffrey Schmidt
 */
public class StatsdMetricAggregated implements GraphiteMetricFormat, OpenTsdbMetricFormat, GenericMetricFormat {
    
    private static final Logger logger = LoggerFactory.getLogger(StatsdMetricAggregated.class.getName());
   
    public static final byte COUNTER_TYPE = 1;
    public static final byte TIMER_TYPE = 2;
    public static final byte GAUGE_TYPE = 3;
    public static final byte SET_TYPE = 4;
    public static final byte UNDEFINED_TYPE = 5;
    
    private Long hashKey_ = null;
    
    private final String bucket_;
    private final BigDecimal metricValue_;
    private final long metricTimestampInMilliseconds_;
    private final long metricTimestampInSeconds_;
    private final byte metricTypeKey_;
    
    public StatsdMetricAggregated(String bucket, BigDecimal metricValue, long metricTimestampInMilliseconds, Byte metricTypeKey) {
        this.bucket_ = bucket;
        this.metricValue_ = metricValue;
        this.metricTimestampInMilliseconds_ = metricTimestampInMilliseconds;
        this.metricTimestampInSeconds_ = metricTimestampInMilliseconds / 1000;
        this.metricTypeKey_ = metricTypeKey;
    }
    
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("");
        
        stringBuilder.append(bucket_).append(" ").append(metricValue_).append(" ").append(metricTimestampInMilliseconds_)
                .append(" ").append(metricTimestampInSeconds_).append(" ").append(metricTypeKey_);

        return stringBuilder.toString();
    }
    
    @Override
    public String getGraphiteFormatString() {
        StringBuilder stringBuilder = new StringBuilder("");
        
        stringBuilder.append(bucket_).append(" ").append(metricValue_).append(" ").append(metricTimestampInSeconds_);

        return stringBuilder.toString();
    }
    
    @Override
    public String getOpenTsdbFormatString() {
        StringBuilder stringBuilder = new StringBuilder("");
        
        stringBuilder.append(bucket_).append(" ").append(metricTimestampInMilliseconds_).append(" ").append(metricValue_).append(" Format=StatsD");

        return stringBuilder.toString();
    }
        
    public Long getHashKey() {
        return this.hashKey_;
    }
    
    @Override
    public Long getMetricHashKey() {
        return getHashKey();
    }
    
    public void setHashKey(Long hashKey) {
        this.hashKey_ = hashKey;
    }
    
    public String getBucket() {
        return bucket_;
    }
    
    @Override
    public String getMetricKey() {
        return getBucket();
    }
    
    public BigDecimal getMetricValue() {
        return metricValue_;
    }
    
    @Override
    public BigDecimal getMetricValueBigDecimal() {
        return getMetricValue();
    }
    
    public long getTimestampInMilliseconds() {
        return metricTimestampInMilliseconds_;
    }
    
    public long getMetricTimestampInSeconds() {
        return metricTimestampInSeconds_;
    }
    
    @Override
    public Long getMetricTimestampInMilliseconds() {
        return getTimestampInMilliseconds();
    }
    
    @Override
    public Long getMetricReceivedTimestampInMilliseconds() {
        return getTimestampInMilliseconds();
    }
    
    public byte getMetricTypeKey() {
        return metricTypeKey_;
    }

}
