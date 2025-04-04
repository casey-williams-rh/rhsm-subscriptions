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
package org.candlepin.subscriptions.metering.service.prometheus.promql;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import org.candlepin.subscriptions.registry.TagMetric;

/**
 * Describes the variables to be applied to a query template. Within a template, these variables can
 * be utilized as follows:
 *
 * <p>#{metric.metricId} #{runtime[yourCustomProperty]}
 */
@Getter
public class QueryDescriptor {

  /** Any variables that should be provided by the tag configuration. */
  private TagMetric metric;

  /** Any variable that are specified at runtime. */
  private Map<String, String> runtime;

  public QueryDescriptor(TagMetric metric) {
    this.metric = metric;
    this.runtime = new HashMap<>();
  }

  public void addRuntimeVar(String name, String value) {
    this.runtime.put(name, value);
  }
}
