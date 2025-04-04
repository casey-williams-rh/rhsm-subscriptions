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
package org.candlepin.subscriptions.subscription;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.stream.Stream;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.candlepin.subscriptions.capacity.files.ProductAllowlist;
import org.candlepin.subscriptions.db.SubscriptionCapacityRepository;
import org.candlepin.subscriptions.db.SubscriptionRepository;
import org.candlepin.subscriptions.db.model.OrgConfigRepository;
import org.candlepin.subscriptions.db.model.Subscription;
import org.candlepin.subscriptions.task.TaskQueueProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Logic for pruning unlisted subscriptions (where the SKU is not in the allowlist). */
@Slf4j
@Component
public class SubscriptionPruneController {
  private final SubscriptionRepository subscriptionRepository;
  private final SubscriptionCapacityRepository subscriptionCapacityRepository;
  private final OrgConfigRepository orgRepository;
  private final Timer pruneAllTimer;
  private final KafkaTemplate<String, PruneSubscriptionsTask>
      pruneSubscriptionsByOrgTaskKafkaTemplate;
  private final String pruneSubscriptionsTopic;
  private final ProductAllowlist productAllowlist;

  @Autowired
  public SubscriptionPruneController(
      SubscriptionRepository subscriptionRepository,
      SubscriptionCapacityRepository subscriptionCapacityRepository,
      OrgConfigRepository orgRepository,
      MeterRegistry meterRegistry,
      KafkaTemplate<String, PruneSubscriptionsTask> pruneSubscriptionsByOrgTaskKafkaTemplate,
      ProductAllowlist productAllowlist,
      @Qualifier("pruneSubscriptionTasks") TaskQueueProperties pruneQueueProperties) {
    this.subscriptionRepository = subscriptionRepository;
    this.subscriptionCapacityRepository = subscriptionCapacityRepository;
    this.orgRepository = orgRepository;
    this.pruneAllTimer = meterRegistry.timer("swatch_subscription_prune_enqueue_all");
    this.productAllowlist = productAllowlist;
    this.pruneSubscriptionsTopic = pruneQueueProperties.getTopic();
    this.pruneSubscriptionsByOrgTaskKafkaTemplate = pruneSubscriptionsByOrgTaskKafkaTemplate;
  }

  public void pruneAllUnlistedSubscriptions() {
    Timer.Sample enqueueAllTime = Timer.start();
    orgRepository.findSyncEnabledOrgs().forEach(this::enqueueSubscriptionPrune);
    Duration enqueueAllDuration = Duration.ofNanos(enqueueAllTime.stop(pruneAllTimer));
    log.info(
        "Enqueued orgs to prune subscriptions in enqueueTimeMillis={}",
        enqueueAllDuration.toMillis());
  }

  @Transactional
  public void pruneUnlistedSubscriptions(String orgId) {
    Stream<Subscription> subscriptions = subscriptionRepository.findByOwnerId(orgId);
    subscriptions.forEach(
        subscription -> {
          if (!productAllowlist.productIdMatches(subscription.getSku())) {
            log.info(
                "Removing subscriptionId={} for orgId={} w/ sku={}",
                subscription.getSubscriptionId(),
                orgId,
                subscription.getSku());
            subscriptionRepository.delete(subscription);
          }
        });
    Stream<org.candlepin.subscriptions.db.model.SubscriptionCapacity> capacityRecords =
        subscriptionCapacityRepository.findByKeyOwnerId(orgId);
    capacityRecords.forEach(
        capacityRecord -> {
          if (!productAllowlist.productIdMatches(capacityRecord.getSku())) {
            log.info(
                "Removing capacity record for subscriptionId={} for orgId={} w/ sku={}",
                capacityRecord.getSubscriptionId(),
                orgId,
                capacityRecord.getSku());
            subscriptionCapacityRepository.delete(capacityRecord);
          }
        });
  }

  private void enqueueSubscriptionPrune(String orgId) {
    log.debug("Enqueuing subscription prune for orgId={}", orgId);
    pruneSubscriptionsByOrgTaskKafkaTemplate.send(
        pruneSubscriptionsTopic, PruneSubscriptionsTask.builder().orgId(orgId).build());
  }
}
