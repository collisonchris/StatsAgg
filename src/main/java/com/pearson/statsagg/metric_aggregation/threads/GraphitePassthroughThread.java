package com.pearson.statsagg.metric_aggregation.threads;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.pearson.statsagg.globals.ApplicationConfiguration;
import com.pearson.statsagg.globals.GlobalVariables;
import com.pearson.statsagg.metric_aggregation.graphite.GraphiteMetricRaw;
import com.pearson.statsagg.modules.GraphiteOutputModule;
import com.pearson.statsagg.modules.OpenTsdbTelnetOutputModule;
import com.pearson.statsagg.utilities.StackTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeffrey Schmidt
 */
public class GraphitePassthroughThread implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(GraphitePassthroughThread.class.getName());
    
    // Lists of active aggregation thread 'thread-start' timestamps. Used as a hacky mechanism for thread blocking on the aggregation threads. 
    private final static List<Long> activeGraphitePassthroughThreadStartGetMetricsTimestamps = Collections.synchronizedList(new ArrayList<Long>());
    
    private final Long threadStartTimestampInMilliseconds_;
    private final String threadId_;
    
    public GraphitePassthroughThread(Long threadStartTimestampInMilliseconds) {
        this.threadStartTimestampInMilliseconds_ = threadStartTimestampInMilliseconds;
        this.threadId_ = "GP-" + threadStartTimestampInMilliseconds_.toString();
    }

    @Override
    public void run() {
        
        if (threadStartTimestampInMilliseconds_ == null) {
            logger.error(this.getClass().getName() + " has invalid initialization value(s)");
            return;
        }
        
        long threadTimeStart = System.currentTimeMillis();
        
        boolean isSuccessfulAdd = activeGraphitePassthroughThreadStartGetMetricsTimestamps.add(threadStartTimestampInMilliseconds_);
        if (!isSuccessfulAdd) {
            logger.error("There is another active thread of type '" + this.getClass().getName() + "' with the same thread start timestamp. Killing this thread...");
            return;
        }
        
        try {  
            // wait until this is the youngest active thread
            int waitInMsCounter = Common.waitUntilThisIsYoungestActiveThread(threadStartTimestampInMilliseconds_, activeGraphitePassthroughThreadStartGetMetricsTimestamps);
            activeGraphitePassthroughThreadStartGetMetricsTimestamps.remove(threadStartTimestampInMilliseconds_);
            
            // get metrics for aggregation
            long createMetricsTimeStart = System.currentTimeMillis();
            List<GraphiteMetricRaw> graphiteMetricsRaw = getCurrentGraphitePassthroughMetricsAndRemoveMetricsFromGlobal();
            long createMetricsTimeElasped = System.currentTimeMillis() - createMetricsTimeStart; 
            
            // prefix graphite metrics (if this option is enabled)
            long prefixMetricsTimeStart = System.currentTimeMillis();
            List<GraphiteMetricRaw> prefixedGraphiteMetricsRaw = GraphiteMetricRaw.createPrefixedGraphiteMetricsRaw(graphiteMetricsRaw, 
                    ApplicationConfiguration.isGlobalMetricNamePrefixEnabled(), ApplicationConfiguration.getGlobalMetricNamePrefixValue(), 
                    ApplicationConfiguration.isGraphitePassthroughMetricNamePrefixEnabled(), ApplicationConfiguration.getGraphitePassthroughMetricNamePrefixValue());
            long prefixMetricsTimeElasped = System.currentTimeMillis() - prefixMetricsTimeStart;  
                    
            // update the global lists of the graphite passthrough's most recent values
            long updateMostRecentDataValueForMetricsTimeStart = System.currentTimeMillis();
            updateMetricMostRecentValues(prefixedGraphiteMetricsRaw);
            long updateMostRecentDataValueForMetricsTimeElasped = System.currentTimeMillis() - updateMostRecentDataValueForMetricsTimeStart; 
            
            // merge current values with the previous window's values (if the application is configured to do this)
            long mergeRecentValuesTimeStart = System.currentTimeMillis();
            List<GraphiteMetricRaw> graphiteMetricsRawMerged = mergePreviousValuesWithCurrentValues(prefixedGraphiteMetricsRaw, 
                    GlobalVariables.graphitePassthroughMetricsMostRecentValue);
            long mergeRecentValuesTimeElasped = System.currentTimeMillis() - mergeRecentValuesTimeStart; 
  
            // updates the global list that tracks the last time a metric was received. 
            long updateMetricLastSeenTimestampTimeStart = System.currentTimeMillis();
            Common.updateMetricLastSeenTimestamps(prefixedGraphiteMetricsRaw);
            Common.updateMetricLastSeenTimestamps_UpdateOnResend(graphiteMetricsRawMerged);
            long updateMetricLastSeenTimestampTimeElasped = System.currentTimeMillis() - updateMetricLastSeenTimestampTimeStart; 
            
            // updates metric value recent value history. this stores the values that are used by the alerting thread.
            long updateAlertMetricKeyRecentValuesTimeStart = System.currentTimeMillis();
            Common.updateAlertMetricRecentValues(graphiteMetricsRawMerged);
            long updateAlertMetricKeyRecentValuesTimeElasped = System.currentTimeMillis() - updateAlertMetricKeyRecentValuesTimeStart; 

            // 'forget' metrics
            long forgetGraphiteMetricsTimeStart = System.currentTimeMillis();
            Common.forgetGenericMetrics(GlobalVariables.forgetGraphitePassthroughMetrics, GlobalVariables.forgetGraphitePassthroughMetricsRegexs, GlobalVariables.graphitePassthroughMetricsMostRecentValue, GlobalVariables.immediateCleanupMetrics);
            long forgetGraphiteMetricsTimeElasped = System.currentTimeMillis() - forgetGraphiteMetricsTimeStart;  
            
            long generateGraphiteStringsTimeElasped = 0;
            if (GraphiteOutputModule.isAnyGraphiteOutputModuleEnabled()) {
                // generate messages for graphite
                long generateGraphiteStringsTimeStart = System.currentTimeMillis();
                List<String> graphiteOutputMessagesForGraphite = GraphiteOutputModule.buildMultiMetricGraphiteMessages(graphiteMetricsRawMerged, ApplicationConfiguration.getGraphiteMaxBatchSize());
                generateGraphiteStringsTimeElasped = System.currentTimeMillis() - generateGraphiteStringsTimeStart; 
            
                // send to graphite
                GraphiteOutputModule.sendMetricsToGraphiteEndpoints(graphiteOutputMessagesForGraphite, threadId_);
            }
            
            if (OpenTsdbTelnetOutputModule.isAnyOpenTsdbOutputModuleEnabled()) {
                // generate messages for OpenTsdb
                List<String> openTsdbOutputMessagesForOpenTsdb = OpenTsdbTelnetOutputModule.buildOpenTsdbMessages(graphiteMetricsRawMerged);
            
                // send to OpenTsdb
                OpenTsdbTelnetOutputModule.sendMetricsToOpenTsdbEndpoints(openTsdbOutputMessagesForOpenTsdb, threadId_);
            }
            
            // total time for this thread took to get & send the graphite metrics
            long threadTimeElasped = System.currentTimeMillis() - threadTimeStart - waitInMsCounter;
            String rate = "0";
            if (threadTimeElasped > 0) {
                rate = Long.toString(graphiteMetricsRaw.size() / threadTimeElasped * 1000);
            }
            
            String aggregationStatistics = "ThreadId=" + threadId_
                    + ", RawMetricCount=" + graphiteMetricsRaw.size() 
                    + ", AggTotalTime=" + threadTimeElasped 
                    + ", MetricsPerSec=" + rate
                    + ", CreateMetricsTime=" + createMetricsTimeElasped
                    + ", PrefixMetricsTime=" + prefixMetricsTimeElasped
                    + ", UpdateRecentValuesTime=" + updateMostRecentDataValueForMetricsTimeElasped 
                    + ", UpdateMetricsLastSeenTime=" + updateMetricLastSeenTimestampTimeElasped 
                    + ", UpdateAlertRecentValuesTime=" + updateAlertMetricKeyRecentValuesTimeElasped
                    + ", MergeNewAndOldMetricsTime=" + mergeRecentValuesTimeElasped
                    + ", NewAndOldMetricCount=" + graphiteMetricsRawMerged.size() 
                    + ", ForgetMetricsTime=" + forgetGraphiteMetricsTimeElasped
                    + ", GenGraphiteStringsTime=" + generateGraphiteStringsTimeElasped;
            
            if (graphiteMetricsRawMerged.isEmpty()) {
                logger.debug(aggregationStatistics);
            }
            else {
                logger.info(aggregationStatistics);
            }
            
            if (ApplicationConfiguration.isDebugModeEnabled()) {
                for (GraphiteMetricRaw graphiteMetricRaw : graphiteMetricsRaw) {
                    logger.info("Graphite metric= " + graphiteMetricRaw.toString());
                }
            }
        }
        catch (Exception e) {
            logger.error(e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }
        
    }
    
    private List<GraphiteMetricRaw> getCurrentGraphitePassthroughMetricsAndRemoveMetricsFromGlobal() {

        if (GlobalVariables.graphitePassthroughMetricsRaw == null) {
            return new ArrayList();
        }

        // gets graphite passthrough metrics for this thread 
        List<GraphiteMetricRaw> graphiteMetricsRaw = new ArrayList(GlobalVariables.graphitePassthroughMetricsRaw.size());
        
        for (GraphiteMetricRaw graphiteMetricRaw : GlobalVariables.graphitePassthroughMetricsRaw.values()) {
            if (graphiteMetricRaw.getMetricReceivedTimestampInMilliseconds() <= threadStartTimestampInMilliseconds_) {
                graphiteMetricsRaw.add(graphiteMetricRaw);
            }
        }
        
        // removes metrics from the global graphite passthrough metrics map (since they are being operated on by this thread)
        for (GraphiteMetricRaw graphiteMetricRaw : graphiteMetricsRaw) {
            GlobalVariables.graphitePassthroughMetricsRaw.remove(graphiteMetricRaw.getHashKey());
        }

        return graphiteMetricsRaw;
    }
    
    private void updateMetricMostRecentValues(List<GraphiteMetricRaw> graphiteMetricsRaw) {
        
        long timestampInMilliseconds = System.currentTimeMillis();
        int timestampInSeconds = (int) (timestampInMilliseconds / 1000);
            
        if (GlobalVariables.graphitePassthroughMetricsMostRecentValue != null) {
            String timestampInSecondsString = Integer.toString(timestampInSeconds);
            
            for (GraphiteMetricRaw graphiteMetricRaw : GlobalVariables.graphitePassthroughMetricsMostRecentValue.values()) {
                GraphiteMetricRaw updatedGraphiteMetricRaw = new GraphiteMetricRaw(graphiteMetricRaw.getMetricPath(), graphiteMetricRaw.getMetricValue(), 
                        timestampInSecondsString, timestampInSeconds, timestampInMilliseconds);
                updatedGraphiteMetricRaw.setHashKey(graphiteMetricRaw.getHashKey());
                
                GlobalVariables.graphitePassthroughMetricsMostRecentValue.put(updatedGraphiteMetricRaw.getMetricPath(), updatedGraphiteMetricRaw);
            }
        }

        if ((graphiteMetricsRaw == null) || graphiteMetricsRaw.isEmpty()) {
            return;
        }
        
        if ((GlobalVariables.graphitePassthroughMetricsMostRecentValue != null) && ApplicationConfiguration.isGraphitePassthroughSendPreviousValue()) {
            Map<String,GraphiteMetricRaw> mostRecentGraphiteMetricsByMetricPath = GraphiteMetricRaw.getMostRecentGraphiteMetricRawByMetricPath(graphiteMetricsRaw);
            
            for (GraphiteMetricRaw graphiteMetricRaw : mostRecentGraphiteMetricsByMetricPath.values()) {
                GlobalVariables.graphitePassthroughMetricsMostRecentValue.put(graphiteMetricRaw.getMetricPath(), graphiteMetricRaw);
            }
        }
            
    }    
    
    private List<GraphiteMetricRaw> mergePreviousValuesWithCurrentValues(List<GraphiteMetricRaw> graphiteMetricsRawNew, 
            Map<String,GraphiteMetricRaw> graphiteMetricsRawOld) {
        
        if ((graphiteMetricsRawNew == null) && (graphiteMetricsRawOld == null)) {
            return new ArrayList<>();
        }
        else if ((graphiteMetricsRawNew == null) && (graphiteMetricsRawOld != null)) {
            return new ArrayList<>(graphiteMetricsRawOld.values());
        }
        else if ((graphiteMetricsRawNew != null) && (graphiteMetricsRawOld == null)) {
            return graphiteMetricsRawNew;
        }
        
        List<GraphiteMetricRaw> graphiteMetricsRawMerged = new ArrayList<>(graphiteMetricsRawNew);
        Map<String,GraphiteMetricRaw> graphiteMetricsRawOldLocal = new HashMap<>(graphiteMetricsRawOld);
        
        for (GraphiteMetricRaw graphiteMetricAggregatedNew : graphiteMetricsRawNew) {
            try {
                graphiteMetricsRawOldLocal.remove(graphiteMetricAggregatedNew.getMetricPath());
            }
            catch (Exception e) {
                logger.error(e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
            } 
        }
        
        graphiteMetricsRawMerged.addAll(graphiteMetricsRawOldLocal.values());
        
        return graphiteMetricsRawMerged;
    }

}
