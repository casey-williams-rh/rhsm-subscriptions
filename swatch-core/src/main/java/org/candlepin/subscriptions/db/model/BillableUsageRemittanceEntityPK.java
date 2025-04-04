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
package org.candlepin.subscriptions.db.model;

import java.io.Serializable;
import java.time.OffsetDateTime;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.candlepin.subscriptions.json.BillableUsage;

@Data
@Embeddable
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BillableUsageRemittanceEntityPK implements Serializable {

  @Column(name = "account_number", nullable = false, length = 32)
  private String accountNumber;

  @Column(name = "product_id", nullable = false, length = 32)
  private String productId;

  @Column(name = "metric_id", nullable = false, length = 32)
  private String metricId;

  @Column(name = "accumulation_period", nullable = false, length = 255)
  private String accumulationPeriod;

  @Column(name = "sla", nullable = false, length = 32)
  private String sla;

  @Column(name = "usage", nullable = false, length = 32)
  private String usage;

  @Column(name = "billing_provider", nullable = false, length = 32)
  private String billingProvider;

  @Column(name = "billing_account_id", nullable = false, length = 255)
  private String billingAccountId;

  public static BillableUsageRemittanceEntityPK keyFrom(BillableUsage billableUsage) {
    return BillableUsageRemittanceEntityPK.builder()
        .usage(billableUsage.getUsage().value())
        .accountNumber(billableUsage.getAccountNumber())
        .billingProvider(billableUsage.getBillingProvider().value())
        .billingAccountId(billableUsage.getBillingAccountId())
        .productId(billableUsage.getProductId())
        .sla(billableUsage.getSla().value())
        .metricId(billableUsage.getUom().value())
        .accumulationPeriod(getAccumulationPeriod(billableUsage.getSnapshotDate()))
        .build();
  }

  public static String getAccumulationPeriod(OffsetDateTime reference) {
    return InstanceMonthlyTotalKey.formatMonthId(reference);
  }
}
