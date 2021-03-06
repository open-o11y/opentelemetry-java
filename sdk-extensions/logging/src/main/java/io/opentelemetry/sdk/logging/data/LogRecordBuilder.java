/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.logging.data;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import java.util.concurrent.TimeUnit;

public final class LogRecordBuilder {
  private long timeUnixNano;
  private String traceId = "";
  private String spanId = "";
  private int flags;
  private LogRecord.Severity severity = LogRecord.Severity.UNDEFINED_SEVERITY_NUMBER;
  private String severityText;
  private String name;
  private AnyValue body = AnyValue.stringAnyValue("");
  private final AttributesBuilder attributeBuilder = Attributes.builder();

  LogRecordBuilder() {}

  public LogRecordBuilder setUnixTimeNano(long timestamp) {
    this.timeUnixNano = timestamp;
    return this;
  }

  public LogRecordBuilder setUnixTimeMillis(long timestamp) {
    return setUnixTimeNano(TimeUnit.MILLISECONDS.toNanos(timestamp));
  }

  public LogRecordBuilder setTraceId(String traceId) {
    this.traceId = traceId;
    return this;
  }

  public LogRecordBuilder setSpanId(String spanId) {
    this.spanId = spanId;
    return this;
  }

  public LogRecordBuilder setFlags(int flags) {
    this.flags = flags;
    return this;
  }

  public LogRecordBuilder setSeverity(LogRecord.Severity severity) {
    this.severity = severity;
    return this;
  }

  public LogRecordBuilder setSeverityText(String severityText) {
    this.severityText = severityText;
    return this;
  }

  public LogRecordBuilder setName(String name) {
    this.name = name;
    return this;
  }

  public LogRecordBuilder setBody(AnyValue body) {
    this.body = body;
    return this;
  }

  public LogRecordBuilder setBody(String body) {
    return setBody(AnyValue.stringAnyValue(body));
  }

  public LogRecordBuilder setAttributes(Attributes attributes) {
    this.attributeBuilder.putAll(attributes);
    return this;
  }

  /**
   * Build a LogRecord instance.
   *
   * @return value object being built
   */
  public LogRecord build() {
    if (timeUnixNano == 0) {
      timeUnixNano = TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());
    }
    return LogRecord.create(
        timeUnixNano,
        traceId,
        spanId,
        flags,
        severity,
        severityText,
        name,
        body,
        attributeBuilder.build());
  }
}
