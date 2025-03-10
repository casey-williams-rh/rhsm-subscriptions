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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.candlepin.subscriptions.db.model.HardwareMeasurementType;
import org.candlepin.subscriptions.json.BillableUsage;
import org.candlepin.subscriptions.json.TallyMeasurement;
import org.candlepin.subscriptions.json.TallyMeasurement.Uom;
import org.candlepin.subscriptions.json.TallySnapshot;
import org.candlepin.subscriptions.json.TallySnapshot.BillingProvider;
import org.candlepin.subscriptions.json.TallySnapshot.Granularity;
import org.candlepin.subscriptions.json.TallySnapshot.Sla;
import org.candlepin.subscriptions.json.TallySnapshot.Usage;
import org.candlepin.subscriptions.json.TallySummary;
import org.candlepin.subscriptions.registry.TagMetaData;
import org.candlepin.subscriptions.registry.TagProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BillableUsageMapperTest {

  private TagProfile tagProfile;

  @BeforeEach
  void setup() {
    tagProfile = new TagProfile();
    TagMetaData tagMetaData = new TagMetaData();
    tagMetaData.setTags(Set.of("rhosak"));
    tagMetaData.setBillingModel("PAYG");
    tagProfile.setTagMetaData(List.of(tagMetaData));
    tagProfile.setTagMetrics(List.of());
    tagProfile.setTagMappings(List.of());
    tagProfile.initLookups();
  }

  @Test
  void shouldSkipNonPaygProducts() {
    BillableUsageMapper mapper = new BillableUsageMapper(tagProfile);
    assertTrue(
        mapper
            .fromTallySummary(
                createExampleTallySummaryWithAccountNumber(
                    "RHEL",
                    Granularity.HOURLY,
                    Sla.STANDARD,
                    Usage.PRODUCTION,
                    BillingProvider.AWS,
                    "123"))
            .findAny()
            .isEmpty());
  }

  @Test
  void shouldSkipAnySla() {
    BillableUsageMapper mapper = new BillableUsageMapper(tagProfile);
    assertTrue(
        mapper
            .fromTallySummary(
                createExampleTallySummaryWithAccountNumber(
                    "rhosak",
                    Granularity.HOURLY,
                    Sla.ANY,
                    Usage.PRODUCTION,
                    BillingProvider.AWS,
                    "123"))
            .findAny()
            .isEmpty());
  }

  @Test
  void shouldSkipAnyUsage() {
    BillableUsageMapper mapper = new BillableUsageMapper(tagProfile);
    assertTrue(
        mapper
            .fromTallySummary(
                createExampleTallySummaryWithAccountNumber(
                    "rhosak",
                    Granularity.HOURLY,
                    Sla.STANDARD,
                    Usage.ANY,
                    BillingProvider.AWS,
                    "123"))
            .findAny()
            .isEmpty());
  }

  @Test
  void shouldSkipAnyBillingProvider() {
    BillableUsageMapper mapper = new BillableUsageMapper(tagProfile);
    assertTrue(
        mapper
            .fromTallySummary(
                createExampleTallySummaryWithAccountNumber(
                    "rhosak",
                    Granularity.HOURLY,
                    Sla.STANDARD,
                    Usage.PRODUCTION,
                    BillingProvider.ANY,
                    "123"))
            .findAny()
            .isEmpty());
  }

  @Test
  void shouldSkipAnyBillingAccountId() {
    BillableUsageMapper mapper = new BillableUsageMapper(tagProfile);
    assertTrue(
        mapper
            .fromTallySummary(
                createExampleTallySummaryWithAccountNumber(
                    "rhosak",
                    Granularity.HOURLY,
                    Sla.STANDARD,
                    Usage.PRODUCTION,
                    BillingProvider.AWS,
                    "_ANY"))
            .findAny()
            .isEmpty());
  }

  @Test
  void shouldProduceBillableUsage_WhenAccountNumberPresent() {
    BillableUsageMapper mapper = new BillableUsageMapper(tagProfile);
    BillableUsage expected =
        new BillableUsage()
            .withAccountNumber("123")
            .withProductId("rhosak")
            .withSnapshotDate(OffsetDateTime.MIN)
            .withUsage(BillableUsage.Usage.PRODUCTION)
            .withSla(BillableUsage.Sla.STANDARD)
            .withBillingProvider(BillableUsage.BillingProvider.AWS)
            .withBillingAccountId("bill123")
            .withUom(BillableUsage.Uom.STORAGE_GIBIBYTES)
            .withValue(42.0);
    assertEquals(
        expected,
        mapper
            .fromTallySummary(
                createExampleTallySummaryWithAccountNumber(
                    "rhosak",
                    Granularity.HOURLY,
                    Sla.STANDARD,
                    Usage.PRODUCTION,
                    BillingProvider.AWS,
                    "bill123"))
            .findAny()
            .orElseThrow());
  }

  @Test
  void shouldProduceBillableUsage_WhenOrgIdPresent() {
    BillableUsageMapper mapper = new BillableUsageMapper(tagProfile);
    BillableUsage expected =
        new BillableUsage()
            .withOrgId("org123")
            .withProductId("rhosak")
            .withSnapshotDate(OffsetDateTime.MIN)
            .withUsage(BillableUsage.Usage.PRODUCTION)
            .withSla(BillableUsage.Sla.STANDARD)
            .withBillingProvider(BillableUsage.BillingProvider.AWS)
            .withBillingAccountId("bill123")
            .withUom(BillableUsage.Uom.STORAGE_GIBIBYTES)
            .withValue(42.0);
    assertEquals(
        expected,
        mapper
            .fromTallySummary(
                createExampleTallySummaryWithOrgId(
                    "rhosak",
                    Granularity.HOURLY,
                    Sla.STANDARD,
                    Usage.PRODUCTION,
                    BillingProvider.AWS,
                    "bill123"))
            .findAny()
            .orElseThrow());
  }

  @Test
  void shouldSkipNonDailySnapshots() {
    BillableUsageMapper mapper = new BillableUsageMapper(tagProfile);
    assertTrue(
        mapper
            .fromTallySummary(
                createExampleTallySummaryWithAccountNumber(
                    "rhosak",
                    Granularity.YEARLY,
                    Sla.STANDARD,
                    Usage.PRODUCTION,
                    BillingProvider.AWS,
                    "123"))
            .findAny()
            .isEmpty());
  }

  @Test
  void shouldSkipSummaryWithNoMeasurements() {
    BillableUsageMapper mapper = new BillableUsageMapper(tagProfile);
    TallySummary tallySummary =
        createExampleTallySummaryWithAccountNumber(
            "rhosak",
            Granularity.HOURLY,
            Sla.STANDARD,
            Usage.PRODUCTION,
            BillingProvider.AWS,
            "123");
    tallySummary.getTallySnapshots().get(0).setTallyMeasurements(null);
    assertTrue(mapper.fromTallySummary(tallySummary).findAny().isEmpty());
  }

  TallySummary createExampleTallySummaryWithAccountNumber(
      String productId,
      Granularity granularity,
      Sla sla,
      Usage usage,
      BillingProvider billingProvider,
      String billingAccountId) {
    return new TallySummary()
        .withAccountNumber("123")
        .withTallySnapshots(
            List.of(
                new TallySnapshot()
                    .withSnapshotDate(OffsetDateTime.MIN)
                    .withProductId(productId)
                    .withGranularity(granularity)
                    .withTallyMeasurements(
                        List.of(
                            new TallyMeasurement()
                                .withUom(Uom.STORAGE_GIBIBYTES)
                                .withHardwareMeasurementType(
                                    HardwareMeasurementType.PHYSICAL.toString())
                                .withValue(42.0),
                            new TallyMeasurement()
                                .withUom(Uom.STORAGE_GIBIBYTES)
                                .withHardwareMeasurementType(
                                    HardwareMeasurementType.TOTAL.toString())
                                .withValue(42.0)))
                    .withSla(sla)
                    .withUsage(usage)
                    .withBillingProvider(billingProvider)
                    .withBillingAccountId(billingAccountId)));
  }

  TallySummary createExampleTallySummaryWithOrgId(
      String productId,
      Granularity granularity,
      Sla sla,
      Usage usage,
      BillingProvider billingProvider,
      String billingAccountId) {
    return new TallySummary()
        .withOrgId("org123")
        .withTallySnapshots(
            List.of(
                new TallySnapshot()
                    .withSnapshotDate(OffsetDateTime.MIN)
                    .withProductId(productId)
                    .withGranularity(granularity)
                    .withTallyMeasurements(
                        List.of(
                            new TallyMeasurement()
                                .withUom(Uom.STORAGE_GIBIBYTES)
                                .withHardwareMeasurementType(
                                    HardwareMeasurementType.PHYSICAL.toString())
                                .withValue(42.0),
                            new TallyMeasurement()
                                .withUom(Uom.STORAGE_GIBIBYTES)
                                .withHardwareMeasurementType(
                                    HardwareMeasurementType.TOTAL.toString())
                                .withValue(42.0)))
                    .withSla(sla)
                    .withUsage(usage)
                    .withBillingProvider(billingProvider)
                    .withBillingAccountId(billingAccountId)));
  }
}
