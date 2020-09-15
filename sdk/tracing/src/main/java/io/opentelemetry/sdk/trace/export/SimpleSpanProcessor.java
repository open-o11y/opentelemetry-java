/*
 * Copyright 2019, OpenTelemetry Authors
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

package io.opentelemetry.sdk.trace.export;

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.export.ConfigBuilder;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of the {@link SpanProcessor} that converts the {@link ReadableSpan} to {@link
 * SpanData} and passes it directly to the configured exporter. This processor should only be used
 * where the exporter(s) are able to handle multiple exports simultaneously, as there is no back
 * pressure consideration here.
 *
 * <p>Configuration options for {@link SimpleSpanProcessor} can be read from system properties,
 * environment variables, or {@link java.util.Properties} objects.
 *
 * <p>For system properties and {@link java.util.Properties} objects, {@link SimpleSpanProcessor}
 * will look for the following names:
 *
 * <ul>
 *   <li>{@code otel.ssp.export.sampled}: sets whether only sampled spans should be exported.
 * </ul>
 *
 * <p>For environment variables, {@link SimpleSpanProcessor} will look for the following names:
 *
 * <ul>
 *   <li>{@code OTEL_SSP_EXPORT_SAMPLED}: sets whether only sampled spans should be exported.
 * </ul>
 *
 * @since 0.5.0
 */
public final class SimpleSpanProcessor implements SpanProcessor {

  private static final Logger logger = Logger.getLogger(SimpleSpanProcessor.class.getName());

  private final SpanExporter spanExporter;
  private final boolean sampled;

  private SimpleSpanProcessor(SpanExporter spanExporter, boolean sampled) {
    this.spanExporter = Objects.requireNonNull(spanExporter, "spanExporter");
    this.sampled = sampled;
  }

  /** @since 0.5.0 */
  @Override
  public void onStart(ReadWriteSpan span) {
    // Do nothing.
  }

  /** @since 0.5.0 */
  @Override
  public boolean isStartRequired() {
    return false;
  }

  /** @since 0.5.0 */
  @Override
  public void onEnd(ReadableSpan span) {
    if (sampled && !span.getSpanContext().isSampled()) {
      return;
    }
    try {
      List<SpanData> spans = Collections.singletonList(span.toSpanData());
      final CompletableResultCode result = spanExporter.export(spans);
      result.whenComplete(
          new Runnable() {
            @Override
            public void run() {
              if (!result.isSuccess()) {
                logger.log(Level.FINE, "Exporter failed");
              }
            }
          });
    } catch (Exception e) {
      logger.log(Level.WARNING, "Exporter threw an Exception", e);
    }
  }

  /** @since 0.5.0 */
  @Override
  public boolean isEndRequired() {
    return true;
  }

  /** @since 0.5.0 */
  @Override
  public CompletableResultCode shutdown() {
    return spanExporter.shutdown();
  }

  /** @since 0.5.0 */
  @Override
  public CompletableResultCode forceFlush() {
    // Do nothing.
    return CompletableResultCode.ofSuccess();
  }

  /**
   * Returns a new Builder for {@link SimpleSpanProcessor}.
   *
   * @param spanExporter the {@code SpanExporter} to where the Spans are pushed.
   * @return a new {@link SimpleSpanProcessor}.
   * @throws NullPointerException if the {@code spanExporter} is {@code null}.
   * @since 0.5.0
   */
  public static Builder newBuilder(SpanExporter spanExporter) {
    return new Builder(spanExporter);
  }

  /** Builder class for {@link SimpleSpanProcessor}. */
  public static final class Builder extends ConfigBuilder<Builder> {

    private static final String KEY_SAMPLED = "otel.ssp.export.sampled";

    @VisibleForTesting static final boolean DEFAULT_EXPORT_ONLY_SAMPLED = true;
    private final SpanExporter spanExporter;
    private boolean exportOnlySampled = DEFAULT_EXPORT_ONLY_SAMPLED;

    private Builder(SpanExporter spanExporter) {
      this.spanExporter = Objects.requireNonNull(spanExporter, "spanExporter");
    }

    /**
     * Sets the configuration values from the given configuration map for only the available keys.
     * This method looks for the following keys:
     *
     * <ul>
     *   <li>{@code otel.ssp.export.sampled}: to set whether only sampled spans should be exported.
     * </ul>
     *
     * @param configMap {@link Map} holding the configuration values.
     * @return this.
     * @since 0.5.0
     */
    @Override
    protected Builder fromConfigMap(
        Map<String, String> configMap, NamingConvention namingConvention) {
      configMap = namingConvention.normalize(configMap);
      Boolean boolValue = getBooleanProperty(KEY_SAMPLED, configMap);
      if (boolValue != null) {
        return this.setExportOnlySampled(boolValue);
      }
      return this;
    }

    /**
     * Set whether only sampled spans should be exported.
     *
     * <p>Default value is {@code true}.
     *
     * @param exportOnlySampled if {@code true} report only sampled spans.
     * @return this.
     * @since 0.5.0
     */
    public Builder setExportOnlySampled(boolean exportOnlySampled) {
      this.exportOnlySampled = exportOnlySampled;
      return this;
    }

    @VisibleForTesting
    boolean getExportOnlySampled() {
      return exportOnlySampled;
    }

    // TODO: Add metrics for total exported spans.
    // TODO: Consider to add support for constant Attributes and/or Resource.

    /**
     * Returns a new {@link SimpleSpanProcessor} that converts spans to proto and forwards them to
     * the given {@code spanExporter}.
     *
     * @return a new {@link SimpleSpanProcessor}.
     * @throws NullPointerException if the {@code spanExporter} is {@code null}.
     * @since 0.5.0
     */
    public SimpleSpanProcessor build() {
      return new SimpleSpanProcessor(spanExporter, exportOnlySampled);
    }
  }
}
