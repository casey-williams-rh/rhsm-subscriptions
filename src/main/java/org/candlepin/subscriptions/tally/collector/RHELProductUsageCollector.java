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
package org.candlepin.subscriptions.tally.collector;

import java.util.Optional;
import org.candlepin.subscriptions.db.model.HardwareMeasurementType;
import org.candlepin.subscriptions.db.model.HostTallyBucket;
import org.candlepin.subscriptions.tally.UsageCalculation;
import org.candlepin.subscriptions.tally.facts.NormalizedFacts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// NOTE: If we need to eventually reuse these rules/calculations for other products
//       we should consider renaming this class.

/** Collects usage data for the RHEL product. */
public class RHELProductUsageCollector implements ProductUsageCollector {

  private static final Logger log = LoggerFactory.getLogger(RHELProductUsageCollector.class);

  @Override
  public Optional<HostTallyBucket> collect(
      UsageCalculation prodCalc, NormalizedFacts normalizedFacts) {
    int appliedCores = normalizedFacts.getCores() != null ? normalizedFacts.getCores() : 0;
    int appliedSockets = normalizedFacts.getSockets() != null ? normalizedFacts.getSockets() : 0;

    boolean guestWithUnknownHypervisor =
        normalizedFacts.isVirtual() && normalizedFacts.isHypervisorUnknown();

    // Cloud provider hosts only account for a single socket.
    if (normalizedFacts.getCloudProviderType() != null) {
      appliedSockets = normalizedFacts.isMarketplace() ? 0 : 1;
      prodCalc.addCloudProvider(
          normalizedFacts.getCloudProviderType(), appliedCores, appliedSockets, 1);
      return Optional.of(
          createBucket(
              prodCalc,
              true,
              appliedCores,
              appliedSockets,
              normalizedFacts.getCloudProviderType()));
    } else if (guestWithUnknownHypervisor) {
      // If the hypervisor is unknown for a guest, we consider it as having a
      // unique hypervisor instance contributing to the hypervisor counts.
      // Since the guest is unmapped, we only contribute a single socket.
      appliedSockets = normalizedFacts.isMarketplace() ? 0 : 1;
      prodCalc.addHypervisor(appliedCores, appliedSockets, 1);
      return Optional.of(
          createBucket(
              prodCalc, true, appliedCores, appliedSockets, HardwareMeasurementType.VIRTUAL));
    }
    // Accumulate for physical systems.
    else if (!normalizedFacts.isVirtual()) {
      // Physical system so increment the physical system counts.
      if (normalizedFacts.isMarketplace()) {
        appliedSockets = 0;
      }
      prodCalc.addPhysical(appliedCores, appliedSockets, 1);
      return Optional.of(
          createBucket(
              prodCalc, true, appliedCores, appliedSockets, HardwareMeasurementType.PHYSICAL));
    }

    // nothing applied to calculation so no bucket to return.
    return Optional.empty();
  }

  @Override
  public Optional<HostTallyBucket> collectForHypervisor(
      String account, UsageCalculation prodCalc, NormalizedFacts hypervisorFacts) {

    int appliedCores = hypervisorFacts.getCores() != null ? hypervisorFacts.getCores() : 0;
    int appliedSockets = hypervisorFacts.getSockets() != null ? hypervisorFacts.getSockets() : 0;

    if (appliedSockets == 0) {
      log.warn(
          "Hypervisor in account {} has no sockets and will"
              + " not contribute to the totals. The tally for the RHEL product will not be accurate since"
              + "all associated guests will not contribute to the tally.",
          account);
    }

    prodCalc.addHypervisor(appliedCores, appliedSockets, 1);
    return Optional.of(
        createBucket(
            prodCalc, false, appliedCores, appliedSockets, HardwareMeasurementType.VIRTUAL));
  }

  private HostTallyBucket createBucket(
      UsageCalculation currentCalc,
      boolean asHypervisor,
      int appliedCores,
      int appliedSockets,
      HardwareMeasurementType appliedType) {
    return new HostTallyBucket(
        null,
        currentCalc.getProductId(),
        currentCalc.getSla(),
        currentCalc.getUsage(),
        currentCalc.getBillingProvider(),
        currentCalc.getBillingAccountId(),
        asHypervisor,
        appliedCores,
        appliedSockets,
        appliedType);
  }
}
