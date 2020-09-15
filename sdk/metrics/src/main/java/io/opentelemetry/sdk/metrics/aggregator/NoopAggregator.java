/*
 * Copyright 2020, OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.sdk.metrics.aggregator;

import io.opentelemetry.common.Labels;
import io.opentelemetry.sdk.metrics.data.MetricData.Point;
import javax.annotation.Nullable;

/** Since 0.3.0 */
public final class NoopAggregator implements Aggregator {
  private static final Aggregator NOOP_AGGREGATOR = new NoopAggregator();
  private static final AggregatorFactory AGGREGATOR_FACTORY =
      new AggregatorFactory() {
        @Override
        public Aggregator getAggregator() {
          return NOOP_AGGREGATOR;
        }
      };

  public static AggregatorFactory getFactory() {
    return AGGREGATOR_FACTORY;
  }

  /** Since 0.3.0 */
  @Override
  public void mergeToAndReset(Aggregator aggregator) {
    // Noop
  }

  /** Since 0.6.0 */
  @Nullable
  @Override
  public Point toPoint(long startEpochNanos, long epochNanos, Labels labels) {
    return null;
  }

  /** Since 0.3.0 */
  @Override
  public void recordLong(long value) {
    // Noop
  }

  /** Since 0.3.0 */
  @Override
  public void recordDouble(double value) {
    // Noop
  }

  @Override
  public boolean hasRecordings() {
    return false;
  }

  private NoopAggregator() {}
}
