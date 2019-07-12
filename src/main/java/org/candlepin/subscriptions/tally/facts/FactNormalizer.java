/*
 * Copyright (c) 2009 - 2019 Red Hat, Inc.
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
package org.candlepin.subscriptions.tally.facts;

import org.candlepin.subscriptions.ApplicationProperties;
import org.candlepin.subscriptions.files.RhelProductListSource;
import org.candlepin.subscriptions.inventory.db.model.InventoryHost;
import org.candlepin.subscriptions.tally.facts.normalizer.FactSetNormalizer;
import org.candlepin.subscriptions.tally.facts.normalizer.QpcFactNormalizer;
import org.candlepin.subscriptions.tally.facts.normalizer.RhsmFactNormalizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


/**
 * Responsible for examining an inventory host and producing normalized
 * and condensed facts based on the host's facts.
 */
public class FactNormalizer {

    private static final Logger log = LoggerFactory.getLogger(FactNormalizer.class);

    private Map<String, FactSetNormalizer> normalizers;

    public FactNormalizer(ApplicationProperties props, RhelProductListSource rhelProductListSource,
        Clock clock) throws IOException {
        normalizers = new HashMap<>();
        normalizers.put(FactSetNamespace.RHSM, new RhsmFactNormalizer(props.getHostLastSyncThresholdHours(),
            rhelProductListSource.list(), clock));
        normalizers.put(FactSetNamespace.QPC, new QpcFactNormalizer());
    }

    /**
     * Normalize the FactSets of the given host.
     *
     * @param host the target host.
     * @return a normalized version of the host's facts.
     */
    public NormalizedFacts normalize(InventoryHost host) {
        NormalizedFacts facts = new NormalizedFacts();
        for (Entry<String, Map<String, Object>> factSet: host.getFacts().entrySet()) {
            if (normalizers.containsKey(factSet.getKey())) {
                log.debug("Normalizing facts for host/namespace: {}/{}", host.getDisplayName(),
                    factSet.getKey());
                normalizers.get(factSet.getKey()).normalize(facts, factSet.getKey(), factSet.getValue());
            }
        }
        return facts;
    }

}
