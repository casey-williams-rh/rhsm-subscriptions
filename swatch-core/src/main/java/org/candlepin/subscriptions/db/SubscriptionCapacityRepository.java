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
package org.candlepin.subscriptions.db;

import java.util.List;
import java.util.stream.Stream;
import org.candlepin.subscriptions.db.model.SubscriptionCapacity;
import org.candlepin.subscriptions.db.model.SubscriptionCapacityKey;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for subscription-provided product capacities. */
public interface SubscriptionCapacityRepository
    extends JpaRepository<SubscriptionCapacity, SubscriptionCapacityKey>,
        CustomizedSubscriptionCapacityRepository {

  List<SubscriptionCapacity> findByKeyOwnerIdAndKeySubscriptionIdIn(
      String ownerId, List<String> subscriptionIds);

  Stream<SubscriptionCapacity> findByKeyOwnerId(String ownerId);

  void deleteByAccountNumber(String accountNumber);
}
