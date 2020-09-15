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

import com.google.errorprone.annotations.concurrent.GuardedBy;
import io.opentelemetry.common.Labels;
import io.opentelemetry.sdk.metrics.data.MetricData.Point;
import io.opentelemetry.sdk.metrics.data.MetricData.SummaryPoint;
import io.opentelemetry.sdk.metrics.data.MetricData.ValueAtPercentile;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/** Since 0.3.0 */
@ThreadSafe
public final class DoubleMinMaxSumCount extends AbstractAggregator {

  private static final AggregatorFactory AGGREGATOR_FACTORY =
      new AggregatorFactory() {
        @Override
        public Aggregator getAggregator() {
          return new DoubleMinMaxSumCount();
        }
      };

  // The current value. This controls its own internal thread-safety via method access. Don't
  // try to use its fields directly.
  private final DoubleSummary current = new DoubleSummary();

  public static AggregatorFactory getFactory() {
    return AGGREGATOR_FACTORY;
  }

  private DoubleMinMaxSumCount() {}

  @Override
  void doMergeAndReset(Aggregator target) {
    DoubleMinMaxSumCount other = (DoubleMinMaxSumCount) target;
    current.mergeAndReset(other.current);
  }

  /** Since 0.3.0 */
  @Nullable
  @Override
  public Point toPoint(long startEpochNanos, long epochNanos, Labels labels) {
    return current.toPoint(startEpochNanos, epochNanos, labels);
  }

  /** Since 0.3.0 */
  @Override
  public void doRecordDouble(double value) {
    current.record(value);
  }

  private static final class DoubleSummary {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @GuardedBy("lock")
    private double sum = 0;

    @GuardedBy("lock")
    private long count = 0;

    @GuardedBy("lock")
    private double min = Double.POSITIVE_INFINITY;

    @GuardedBy("lock")
    private double max = Double.NEGATIVE_INFINITY;

    private void record(double value) {
      lock.writeLock().lock();
      try {
        count++;
        sum += value;
        min = Math.min(value, min);
        max = Math.max(value, max);
      } finally {
        lock.writeLock().unlock();
      }
    }

    /** Since 0.3.0 */
    public void mergeAndReset(DoubleSummary other) {
      long myCount;
      double mySum;
      double myMin;
      double myMax;
      lock.writeLock().lock();
      try {
        if (this.count == 0) {
          return;
        }
        myCount = this.count;
        mySum = this.sum;
        myMin = this.min;
        myMax = this.max;
        this.count = 0;
        this.sum = 0;
        this.min = Double.POSITIVE_INFINITY;
        this.max = Double.NEGATIVE_INFINITY;
      } finally {
        lock.writeLock().unlock();
      }
      other.lock.writeLock().lock();
      try {
        other.count += myCount;
        other.sum += mySum;
        other.min = Math.min(myMin, other.min);
        other.max = Math.max(myMax, other.max);
      } finally {
        other.lock.writeLock().unlock();
      }
    }

    /** Since 0.6.0 */
    @Nullable
    private SummaryPoint toPoint(long startEpochNanos, long epochNanos, Labels labels) {
      lock.readLock().lock();
      try {
        return count == 0
            ? null
            : SummaryPoint.create(
                startEpochNanos,
                epochNanos,
                labels,
                count,
                sum,
                Arrays.asList(
                    ValueAtPercentile.create(0.0, min), ValueAtPercentile.create(100.0, max)));
      } finally {
        lock.readLock().unlock();
      }
    }
  }
}
