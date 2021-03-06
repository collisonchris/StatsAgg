package com.pearson.statsagg.webui;

import java.io.PrintWriter;
import java.math.BigDecimal;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.pearson.statsagg.database.alerts.Alert;
import com.pearson.statsagg.database.alerts.AlertsDao;
import com.pearson.statsagg.database.metric_group.MetricGroup;
import com.pearson.statsagg.database.metric_group.MetricGroupsDao;
import com.pearson.statsagg.database.notifications.NotificationGroup;
import com.pearson.statsagg.database.notifications.NotificationGroupsDao;
import com.pearson.statsagg.globals.GlobalVariables;
import com.pearson.statsagg.utilities.DateAndTime;
import com.pearson.statsagg.utilities.StackTrace;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeffrey Schmidt
 */
@WebServlet(name = "AlertDetails", urlPatterns = {"/AlertDetails"})
public class AlertDetails extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(AlertDetails.class.getName());
    
    public static final String PAGE_NAME = "Alert Details";
    
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        processGetRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        processGetRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return PAGE_NAME;
    }
    
    protected void processGetRequest(HttpServletRequest request, HttpServletResponse response) {
        
        if ((request == null) || (response == null)) {
            return;
        }
        
        response.setContentType("text/html");
        PrintWriter out = null;
    
        String alertName = request.getParameter("Name");
        String alertDetails = getAlertDetailsString(alertName);
                
        try {  
            StringBuilder htmlBuilder = new StringBuilder("");

            StatsAggHtmlFramework statsAggHtmlFramework = new StatsAggHtmlFramework();
            String htmlHeader = statsAggHtmlFramework.createHtmlHeader("StatsAgg - " + PAGE_NAME, "");
            
            String htmlBody = statsAggHtmlFramework.createHtmlBody(
            "<div id=\"page-content-wrapper\">\n" +
            "  <!-- Keep all page content within the page-content inset div! -->\n" +
            "  <div class=\"page-content inset\" style=\"font-size:12px;\">\n" +
            "    <div class=\"content-header\"> \n" +
            "      <div class=\"pull-left content-header-h2-min-width-statsagg\"> <h2> " + PAGE_NAME + " </h2> </div>\n" +
            "    </div>\n " +
            "    <div class=\"row create-alert-form-row\">\n" +
            alertDetails +
            "  </div></div>\n" +
            "</div>\n");
            
            htmlBuilder.append("<!DOCTYPE html>\n<html>\n").append(htmlHeader).append(htmlBody).append("</html>");
            
            Document htmlDocument = Jsoup.parse(htmlBuilder.toString());
            String htmlFormatted  = htmlDocument.toString();
            out = response.getWriter();
            out.println(htmlFormatted);
        }
        catch (Exception e) {
            logger.error(e.toString() + System.lineSeparator() + StackTrace.getStringFromStackTrace(e));
        }
        finally {            
            if (out != null) {
                out.close();
            }
        }
        
    }

    private String getAlertDetailsString(String alertName) {
        
        if (alertName == null) {
            return "";
        }
        
        StringBuilder outputString = new StringBuilder("");
        
        AlertsDao alertsDao = new AlertsDao();
        Alert alert = alertsDao.getAlertByName(alertName);
        
        if (alert != null) {
            NotificationGroup cautionNotificationGroup = null, dangerNotificationGroup = null;
            NotificationGroupsDao notificationGroupsDao = new NotificationGroupsDao(false);
            if (alert.getCautionNotificationGroupId() != null) cautionNotificationGroup = notificationGroupsDao.getNotificationGroup(alert.getCautionNotificationGroupId());
            if (alert.getDangerNotificationGroupId() != null) dangerNotificationGroup = notificationGroupsDao.getNotificationGroup(alert.getDangerNotificationGroupId());
            notificationGroupsDao.close();
            
            MetricGroup metricGroup = null;
            MetricGroupsDao metricGroupsDao = new MetricGroupsDao();
            if (alert.getMetricGroupId() != null) metricGroup = metricGroupsDao.getMetricGroup(alert.getMetricGroupId());
            
            outputString.append("<div class=\"col-md-4\">\n");
            outputString.append("<div class=\"panel panel-default\"> <div class=\"panel-heading\"><b>Core Details</b></div> <div class=\"panel-body\">");
            
            outputString.append("<b>Name</b> = ").append(StatsAggHtmlFramework.htmlEncode(alert.getName())).append("<br>");
            
            outputString.append("<b>Description</b> = ");
            if (alert.getDescription() != null) {
                String encodedAlertDescription = StatsAggHtmlFramework.htmlEncode(alert.getDescription());
                outputString.append(encodedAlertDescription.replaceAll("\n", "<br>")).append("<br><br>");
            }
            else outputString.append("<br><br>");
            
            outputString.append("<b>Metric group name</b> = ");
            if (metricGroup != null) {
                String metricGroupDetails = "<a href=\"MetricGroupDetails?Name=" + StatsAggHtmlFramework.urlEncode(metricGroup.getName()) + "\">" + 
                    StatsAggHtmlFramework.htmlEncode(metricGroup.getName()) + "</a>";
                outputString.append(metricGroupDetails).append("<br>");
            }
            else outputString.append("<br>");
                    
            outputString.append("<b>Is alert enabled?</b> = ").append(alert.isEnabled()).append("<br>");
            
            outputString.append("<b>Is caution alerting enabled?</b> = ").append(alert.isCautionEnabled()).append("<br>");
            
            outputString.append("<b>Is danger alerting enabled?</b> = ").append(alert.isDangerEnabled()).append("<br>");
            
            outputString.append("<b>Alert type</b> = ");
            if (alert.getAlertType() != null) {
                if (alert.getAlertType() == Alert.TYPE_AVAILABILITY) outputString.append("Availability").append("<br>");
                else if (alert.getAlertType() == Alert.TYPE_THRESHOLD) outputString.append("Threshold").append("<br>");
                else outputString.append("N/A").append("<br>");
            }
            else outputString.append("N/A <br>");
            
            outputString.append("<b>Alert on positive?</b> = ").append(alert.isAlertOnPositive()).append("<br>");
            outputString.append("<b>Resend Alert?</b> = ").append(alert.isAllowResendAlert()).append("<br>");
            
            outputString.append("<b>Resent alert every...</b> = ");
            if (alert.getSendAlertEveryNumMilliseconds() != null) {
                BigDecimal resendAlertMs = new BigDecimal(alert.getSendAlertEveryNumMilliseconds());
                BigDecimal resendAlertSeconds = resendAlertMs.divide(new BigDecimal(1000));
                outputString.append(resendAlertSeconds.stripTrailingZeros().toPlainString()).append(" seconds<br>");
            }
            else outputString.append("N/A <br>");
            
            outputString.append("<br>");
            
            boolean isAlertSuspended = false;
            outputString.append("<b>Is alert suspended?</b> = ");
            if ((GlobalVariables.alertSuspensionStatusByAlertId != null) && (GlobalVariables.alertSuspensionStatusByAlertId.get(alert.getId()) != null) &&
                    GlobalVariables.alertSuspensionStatusByAlertId.get(alert.getId())) {
                outputString.append("yes <br>");
                isAlertSuspended = true;
            }
            else outputString.append("no <br>");
            
            outputString.append("<b>Is alert suspended, notification only?</b> = ");
            if (isAlertSuspended && (GlobalVariables.alertSuspensionLevelsByAlertId != null) && (GlobalVariables.alertSuspensionLevelsByAlertId.get(alert.getId()) != null)) {
                Integer suspensionLevel = GlobalVariables.alertSuspensionLevelsByAlertId.get(alert.getId());
                
                if (com.pearson.statsagg.alerts.AlertSuspensions.SUSPEND_ALERT_NOTIFICATION_ONLY == suspensionLevel) outputString.append("yes <br>");
                else outputString.append("no <br>");
            }
            else outputString.append("N/A <br>");
            
            outputString.append("</div></div></div>").append("<div class=\"col-md-4\">\n");
            outputString.append("<div class=\"panel panel-warning\"> <div class=\"panel-heading\"><b>Caution Details</b></div> <div class=\"panel-body\">");
                        
            if ((alert.isCautionEnabled() != null) && !alert.isCautionEnabled()) outputString.append("<del>");
            
            outputString.append("<b>Caution notification group name</b> = ");
            if (cautionNotificationGroup != null) {
                String notificationGroupDetails = "<a href=\"NotificationGroupDetails?Name=" + StatsAggHtmlFramework.urlEncode(cautionNotificationGroup.getName()) + "\">" + 
                        StatsAggHtmlFramework.htmlEncode(cautionNotificationGroup.getName()) + "</a>";
                outputString.append(notificationGroupDetails).append("<br>");
            }
            else outputString.append("N/A <br>");
            
            outputString.append("<b>Caution window duration</b> = ");
            if (alert.getCautionWindowDuration() != null) {
                BigDecimal cautionWindowDurationMs = new BigDecimal(alert.getCautionWindowDuration());
                BigDecimal cautionWindowDurationSeconds = cautionWindowDurationMs.divide(new BigDecimal(1000));
                outputString.append(cautionWindowDurationSeconds.stripTrailingZeros().toPlainString()).append(" seconds<br>");
            }
            else outputString.append("N/A <br>");
            
            if ((alert.getAlertType() != null) && (alert.getAlertType() != Alert.TYPE_AVAILABILITY && ((alert.isCautionEnabled() != null) && alert.isCautionEnabled()))) outputString.append("<del>");
            
            outputString.append("<b>Caution stop tracking after...</b> = ");
            if (alert.getCautionStopTrackingAfter() != null) {
                BigDecimal cautionStopTrackingAfterMs = new BigDecimal(alert.getCautionStopTrackingAfter());
                BigDecimal cautionStopTrackingAfterSeconds = cautionStopTrackingAfterMs.divide(new BigDecimal(1000));
                outputString.append(cautionStopTrackingAfterSeconds.stripTrailingZeros().toPlainString()).append(" seconds<br>");
            }
            else outputString.append("N/A <br>");
            
            if ((alert.getAlertType() != null) && (alert.getAlertType() != Alert.TYPE_AVAILABILITY) && ((alert.isCautionEnabled() != null) && alert.isCautionEnabled())) outputString.append("</del>");
          
            if ((alert.getAlertType() != null) && (alert.getAlertType() != Alert.TYPE_THRESHOLD) && ((alert.isCautionEnabled() != null) && alert.isCautionEnabled())) outputString.append("<del>");
            
            outputString.append("<b>Caution minimum sample count</b> = ");
            if (alert.getCautionMinimumSampleCount() != null) outputString.append(alert.getCautionMinimumSampleCount()).append("<br>");
            else outputString.append("N/A <br>");
            
            outputString.append("<b>Caution operator</b> = ");
            if (alert.getCautionOperatorString(true, true) != null) outputString.append("'").append(alert.getCautionOperatorString(true, true)).append("'<br>");
            else outputString.append("N/A <br>");
            
            outputString.append("<b>Caution combination</b> = ");
            if (alert.getCautionCombinationString() != null) outputString.append(alert.getCautionCombinationString()).append("<br>");
            else outputString.append("N/A <br>");
            
            outputString.append("<b>Caution combination count</b> = ");
            if (alert.getCautionCombinationCount() != null) outputString.append(alert.getCautionCombinationCount()).append("<br>");
            else outputString.append("N/A <br>");
            
            outputString.append("<b>Caution threshold</b> = ");
            if (alert.getCautionThreshold() != null) outputString.append(alert.getCautionThreshold().stripTrailingZeros().toPlainString()).append("<br>");
            else outputString.append("N/A <br>");
            
            if ((alert.getAlertType() != null) && (alert.getAlertType() != Alert.TYPE_THRESHOLD && ((alert.isCautionEnabled() != null) && alert.isCautionEnabled()))) outputString.append("</del>");
            
            outputString.append("<b>Are caution configuration settings valid?</b> = ").append(alert.isCautionAlertCriteriaValid()).append("<br>");
            
            outputString.append("<b>Caution alert active?</b> = ").append(alert.isCautionAlertActive()).append("<br>");
            
            outputString.append("<b>Caution acknowledged?</b> = ");
            if (alert.isCautionAcknowledged() != null) outputString.append(alert.isCautionAcknowledged()).append("<br>");
            else outputString.append("N/A <br>");
            
            outputString.append("<b>Caution alert last notification timestamp</b> = ");
            if (alert.getCautionAlertLastSentTimestamp() != null) outputString.append(DateAndTime.getFormattedDateAndTime(alert.getCautionAlertLastSentTimestamp(), "yyyy-MM-dd, h:mm:ss a")).append("<br>");
            else outputString.append("N/A <br>");
            
            outputString.append("<b>Is caution alert status pending? (caused by application restart) </b> = ");
            if ((alert.getId() != null) && (GlobalVariables.pendingCautionAlertsByAlertId != null) && GlobalVariables.pendingCautionAlertsByAlertId.containsKey(alert.getId())) {
                outputString.append("yes <br>");  
            }
            else outputString.append("no <br>");  
            
            if ((alert.isCautionEnabled() != null) && !alert.isCautionEnabled()) outputString.append("</del>");

            outputString.append("</div></div></div>").append("<div class=\"col-md-4\">\n");
            outputString.append("<div class=\"panel panel-danger\"> <div class=\"panel-heading\"><b>Danger Details</b></div> <div class=\"panel-body\">");

            if ((alert.isDangerEnabled() != null) && !alert.isDangerEnabled()) outputString.append("<del>");
            
            outputString.append("<b>Danger notification group name</b> = ");
            if (dangerNotificationGroup != null) {
                String notificationGroupDetails = "<a href=\"NotificationGroupDetails?Name=" + StatsAggHtmlFramework.urlEncode(dangerNotificationGroup.getName()) + "\">" + 
                        StatsAggHtmlFramework.htmlEncode(dangerNotificationGroup.getName()) + "</a>";
                outputString.append(notificationGroupDetails).append("<br>");
            }
            else outputString.append("N/A <br>");
            
            outputString.append("<b>Danger window duration</b> = ");
            if (alert.getDangerWindowDuration() != null) {
                BigDecimal dangerWindowDurationMs = new BigDecimal(alert.getDangerWindowDuration());
                BigDecimal dangerWindowDurationSeconds = dangerWindowDurationMs.divide(new BigDecimal(1000));
                outputString.append(dangerWindowDurationSeconds.stripTrailingZeros().toPlainString()).append(" seconds<br>");
            }
            else outputString.append("N/A <br>");
            
            if ((alert.getAlertType() != null) && (alert.getAlertType() != Alert.TYPE_AVAILABILITY) && ((alert.isDangerEnabled() != null) && alert.isDangerEnabled())) outputString.append("<del>");
            
            outputString.append("<b>Danger stop tracking after...</b> = ");
            if (alert.getDangerStopTrackingAfter() != null) {
                BigDecimal dangerStopTrackingAfterMs = new BigDecimal(alert.getDangerStopTrackingAfter());
                BigDecimal dangerStopTrackingAfterSeconds = dangerStopTrackingAfterMs.divide(new BigDecimal(1000));
                outputString.append(dangerStopTrackingAfterSeconds.stripTrailingZeros().toPlainString()).append(" seconds<br>");
            }
            else outputString.append("N/A <br>");
            
            if ((alert.getAlertType() != null) && (alert.getAlertType() != Alert.TYPE_AVAILABILITY) && ((alert.isDangerEnabled() != null) && alert.isDangerEnabled())) outputString.append("</del>");
            
            if ((alert.getAlertType() != null) && (alert.getAlertType() != Alert.TYPE_THRESHOLD) && ((alert.isDangerEnabled() != null) && alert.isDangerEnabled())) outputString.append("<del>");
            
            outputString.append("<b>Danger minimum sample count</b> = ");
            if (alert.getDangerMinimumSampleCount() != null) outputString.append(alert.getDangerMinimumSampleCount()).append("<br>");
            else outputString.append("N/A <br>");
            
            outputString.append("<b>Danger operator</b> = ");
            if (alert.getDangerOperatorString(true, true) != null) outputString.append("'").append(alert.getDangerOperatorString(true, true)).append("'<br>");
            else outputString.append("N/A <br>");
            
            outputString.append("<b>Danger combination</b> = ");
            if (alert.getDangerCombinationString() != null) outputString.append(alert.getDangerCombinationString()).append("<br>");
            else outputString.append("N/A <br>");
            
            outputString.append("<b>Danger combination count</b> = ");
            if (alert.getDangerCombinationCount() != null) outputString.append(alert.getDangerCombinationCount()).append("<br>");
            else outputString.append("N/A <br>");
            
            outputString.append("<b>Danger threshold</b> = ");
            if (alert.getDangerThreshold() != null) outputString.append(alert.getDangerThreshold().stripTrailingZeros().toPlainString()).append("<br>");
            else outputString.append("N/A <br>");
            
            if ((alert.getAlertType() != null) && (alert.getAlertType() != Alert.TYPE_THRESHOLD) && ((alert.isDangerEnabled() != null) && alert.isDangerEnabled())) outputString.append("</del>");
            
            outputString.append("<b>Are danger configuration settings valid?</b> = ").append(alert.isDangerAlertCriteriaValid()).append("<br>");
            
            outputString.append("<b>Danger alert active?</b> = ").append(alert.isDangerAlertActive()).append("<br>");
            
            outputString.append("<b>Danger acknowledged?</b> = ");
            if (alert.isDangerAcknowledged() != null) outputString.append(alert.isDangerAcknowledged()).append("<br>");
            else outputString.append("N/A <br>");
            
            outputString.append("<b>Danger alert last notification timestamp</b> = ");
            if (alert.getDangerAlertLastSentTimestamp() != null) outputString.append(DateAndTime.getFormattedDateAndTime(alert.getDangerAlertLastSentTimestamp(), "yyyy-MM-dd, h:mm:ss a")).append("<br>");
            else outputString.append("N/A <br>");
            
            outputString.append("<b>Is danger alert status pending? (caused by application restart) </b> = ");
            if ((alert.getId() != null) && (GlobalVariables.pendingDangerAlertsByAlertId != null) && GlobalVariables.pendingDangerAlertsByAlertId.containsKey(alert.getId())) {
                outputString.append("yes <br>");  
            }
            else outputString.append("no <br>");  
            
            if ((alert.isDangerEnabled() != null) && !alert.isDangerEnabled()) outputString.append("</del>");
            
            outputString.append("</div></div></div>");
        }
        
        return outputString.toString();
    }

}
