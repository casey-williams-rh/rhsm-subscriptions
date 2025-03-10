/*
 * Copyright Red Hat, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Red Hat trademarks are not licensed under GPLv3. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.subscriptions.metering.job;

import org.candlepin.subscriptions.exception.JobFailureException;
import org.candlepin.subscriptions.metering.service.prometheus.MetricProperties;
import org.candlepin.subscriptions.metering.service.prometheus.task.PrometheusMetricsTaskManager;
import org.candlepin.subscriptions.registry.TagProfile;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Scheduled;

/** A cron job that sends a task message to capture metrics from prometheus for metering. */
public class MeteringJob implements Runnable {

  private PrometheusMetricsTaskManager tasks;
  private final TagProfile tagProfile;
  private MetricProperties metricProperties;
  private RetryTemplate retryTemplate;

  public MeteringJob(
      PrometheusMetricsTaskManager tasks,
      TagProfile tagProfile,
      MetricProperties metricProperties,
      RetryTemplate retryTemplate) {
    this.tasks = tasks;
    this.tagProfile = tagProfile;
    this.metricProperties = metricProperties;
    this.retryTemplate = retryTemplate;
  }

  @Override
  @Scheduled(cron = "${rhsm-subscriptions.jobs.metering-schedule}")
  public void run() {
    int range = metricProperties.getRangeInMinutes();
    for (String productTag : tagProfile.getTagsWithPrometheusEnabledLookup()) {
      try {
        tasks.updateMetricsForAllAccounts(productTag, range, retryTemplate);
      } catch (Exception e) {
        throw new JobFailureException("Unable to run MeteringJob.", e);
      }
    }
  }
}
