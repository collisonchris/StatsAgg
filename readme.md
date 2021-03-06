# StatsAgg

## Overview
StatsAgg is a metric aggregation and alerting platform. It currently accepts Graphite-formatted metrics and StatsD formatted metrics.

StatsAgg works by receiving Graphite & StatsD metrics, aggregating them, alerting on them, and outputting the aggregated metrics to a Graphite-compatible metric storage platform. The diagram (linked to below) shows a typical deployment & use-case pattern for StatsAgg.

[StatsAgg component diagram](./docs/component-diagram.png)

<br>
## What are StatsAgg's core features?
* A complete re-implementation of StatsD
    * TCP & UDP support (both can run concurrently).
    * Support for all metric-types.
* Receives (and optionally, aggregates) Graphite metrics
    * TCP & UDP support (both can run concurrently).
    * Aggregates into minimum, average, maximum for the values of an individual metric-path (during an 'aggregation window').
* Outputs aggregated metrics to Graphite (and Graphite compatible services)
* A robust alerting mechanism 
    * Can alert off of any received/aggregated metric.
    * Regular-expression based mechanism for tying together metrics & alerts.
    * 'threshold' based alerting, or 'availability' based alerting.
    * A flexible alert-suspension mechanism.
    * Alerts notifications can be sent via email, or viewed in the StatsAgg website.
* A web-based UI for managing alerts & metrics

A more detailed discussion of StatsAgg's features can be found in the [StatsAgg user manual](./docs/manual.pdf)

<br>
## Why should I use StatsAgg?
StatsAgg was originally written to fill some gaps that in some other popular open-source monitoring tools. Specifically...

* Graphite and StatsD do not have a native alerting mechanism
    * Most alerting solutions for StatsD and/or Graphite metrics are provided by (expensive) SaaS venders.
    * The alerting mechanism in StatsAgg compares favourably to many pay-based solutions.
* Provides an alternative way of managing servers/services/etc compared to tools like Nagios, Zabbix, etc
    * StatsAgg allows you to break away from viewing everything from the perspective of servers/hosts. You can structure your metrics in such a way so as to group everything by host, but you aren't required to.
    * Generally speaking, tools like Nagios, Zabbix, etc lack the ability to alert off of free-form metrics. Since StatsAgg uses regular-expressions to tie metrics to alerts, you can just as easily alert off of a 'free-form metric hierarchy' as you can a 'highly structured metric hierarchy'.
    * Tools like Nagios, Zabbix, etc aren't built around having fine datapoint granularity (more frequent than once per minute). StatsAgg was written to be able to receive, aggregate, alert on, and output metrics that are sent at any interval.
* StatsD limitations
    * StatsD doesn't allow the TCP server & the UDP server to be active at the same time. StatsAgg does.
    * StatsD doesn't persist Gauge metrics, so a restart will result of StatsD 'forgetting' what the previous Gauge metric values were. StatsAgg can persists Gauge values, so a restart won't result in Gauges resetting themselves.
* StatsAgg provides a web-based UI for managing alerts & metrics.
* Performance
    * StatsAgg is Java-based, and has been thoroughly tuned for performance.
    * A server with 2 cpu cores & 4 gigabytes of RAM should have no trouble processing thousands of metrics per second.

<br>
## Screenshots
* [Homepage](./docs/home.png)
* [Alerts List](./docs/alerts.png)
* [Create an Alert](./docs/create_alert.png)
* [A preview of an email alert](./docs/preview_alert.png)
* [Alert Suspensions List](./docs/alert_suspensions.png)
* [Create an Alert Suspension](./docs/create_suspension.png)
* [Metric Groups List](./docs/metric_groups.png)
* [Create a Metric Group](./docs/create_metricgroup.png)
* [Notification Groups List](./docs/notification_groups.png)

<br>
## Installation
Detailed installation instructions can be found in the [StatsAgg user manual](./docs/manual.pdf)

<br>
## Example programs/frameworks/etc that are compatible with StatsAgg
* [Java Metrics](https://dropwizard.github.io/metrics)
* [CollectD](https://collectd.org/)
* StatsPoller -- a Pearson-developed metrics collection agent for servers (to be released sometime in 2015).
* Anything that can output in Graphite or StatsD format

<br>
## Accepted input formats
Detailed information about StatsAgg's metric format support, including examples, can be found in the  [StatsAgg user manual](./docs/manual.pdf)

<br>
## Technology
* StatsAgg is a Java 1.7 based webapp. It compiles into a war file, and is intended to be deployed into Apache Tomcat 7+.
* StatsAgg uses a database for storing things like 'StatsD gauge values', alert definitions & statuses, metric group definitions, etc. The database technology can be Apache Derby Embedded, or MySQL 5.6+.
* StatsAgg can run on almost any modern OS. Windows, Linux, etc.

<br>
## Limitations
* StatsAgg only supports running in a single-server configuration. 
    * While this is a limitation, a lot of time/energy was put into tuning StatsAgg's performance. For *most* implementations, a single StatsAgg server should be adequate. 
* A few StatsD features that are in the main StatsD program are missing in StatsAgg. Missing features include...
    * histograms on timers (will be included in a future build).
    * configuring StatsAgg to be a 'repeater' (you can use the official StatsD program to do this, and forward to StatsAgg).
    * configuring StatsAgg to be in a 'proxy cluster' configuration (you can use the official StatsD program to do this, and forward to StatsAgg). 
    * outputting frequently sent metric-keys to log files.
* StatsAgg is not (currently) meant for use as an incident management tool. It doesn't support alert history, alert acknowledgement, etc. 
* StatsAgg is not a metric visualization tool. However, StatsAgg works well when paired with Graphite/Grafana for visualization.
* StatsAgg does not currently support the Graphite 'Pickle' format (will be included in a future build).

<br>
## Thanks to...
* StatsD : etsy @ https://github.com/etsy/statsd/
* Graphite : Orbitz @ http://graphite.wikidot.com/
* Pearson Assessments, a division of Pearson Education: http://www.pearsonassessments.com/