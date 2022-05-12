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
package org.candlepin.subscriptions.tally.billing;

import lombok.extern.slf4j.Slf4j;
import org.candlepin.subscriptions.json.TallySummary;
import org.candlepin.subscriptions.task.TaskQueueProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Component that produces billing usage messages based on TallySnapshots.
 *
 * <p>NOTE: We are currently just forwarding TallySummary messages, but will transition to sending
 * BillingUsage.
 */
@Service
@Slf4j
public class BillingProducer {

  private KafkaTemplate<String, TallySummary> billingUsageKafkaTemplate;
  private String billingUsageTopic;

  @Autowired
  public BillingProducer(
      @Qualifier("billingUsageTopicProperties") TaskQueueProperties billingUsageTopicProperties,
      @Qualifier("billingUsageKafkaTemplate")
          KafkaTemplate<String, TallySummary> billingUsageKafkaTemplate) {
    this.billingUsageKafkaTemplate = billingUsageKafkaTemplate;
    this.billingUsageTopic = billingUsageTopicProperties.getTopic();
  }

  public void produce(TallySummary tallySummary) {
    log.debug(
        "Forwarding summary {} to topic {}",
        tallySummary.getAccountNumber(),
        this.billingUsageTopic);
    billingUsageKafkaTemplate.send(this.billingUsageTopic, tallySummary);
  }
}
