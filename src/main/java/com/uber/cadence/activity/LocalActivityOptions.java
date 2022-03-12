/*
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.uber.cadence.activity;

import static com.uber.cadence.internal.common.OptionsUtils.roundUpToSeconds;

import com.uber.cadence.common.MethodRetry;
import com.uber.cadence.common.RetryOptions;
import com.uber.cadence.context.ContextPropagator;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/** Options used to configure how an local activity is invoked. */
public final class LocalActivityOptions {

  /**
   * Used to merge annotation and options. Options takes precedence. Returns options with all
   * defaults filled in.
   */
  public static LocalActivityOptions merge(
      ActivityMethod a, MethodRetry r, LocalActivityOptions o) {
    if (a == null) {
      if (r == null) {
        return new LocalActivityOptions.Builder(o).validateAndBuildWithDefaults();
      }
      RetryOptions mergedR = RetryOptions.merge(r, o.getRetryOptions());
      return new LocalActivityOptions.Builder()
          .setRetryOptions(mergedR)
          .validateAndBuildWithDefaults();
    }
    if (o == null) {
      o = new LocalActivityOptions.Builder().build();
    }
    return new LocalActivityOptions.Builder()
        .setScheduleToCloseTimeout(
            ActivityOptions.mergeDuration(
                a.scheduleToCloseTimeoutSeconds(), o.getScheduleToCloseTimeout()))
        .setRetryOptions(RetryOptions.merge(r, o.getRetryOptions()))
        .setContextPropagators(o.getContextPropagators())
        .validateAndBuildWithDefaults();
  }

  public static final class Builder {
    private Duration scheduleToCloseTimeout;
    private RetryOptions retryOptions;
    private List<ContextPropagator> contextPropagators;

    public Builder() {}

    /** Copy Builder fields from the options. */
    public Builder(LocalActivityOptions options) {
      if (options == null) {
        return;
      }
      this.scheduleToCloseTimeout = options.getScheduleToCloseTimeout();
      this.retryOptions = options.retryOptions;
    }

    /** Overall timeout workflow is willing to wait for activity to complete. */
    public Builder setScheduleToCloseTimeout(Duration scheduleToCloseTimeout) {
      this.scheduleToCloseTimeout = scheduleToCloseTimeout;
      return this;
    }

    /**
     * RetryOptions that define how activity is retried in case of failure. Default is null which is
     * no reties.
     */
    public Builder setRetryOptions(RetryOptions retryOptions) {
      this.retryOptions = retryOptions;
      return this;
    }

    public Builder setContextPropagators(List<ContextPropagator> contextPropagators) {
      this.contextPropagators = contextPropagators;
      return this;
    }

    public LocalActivityOptions build() {
      return new LocalActivityOptions(scheduleToCloseTimeout, retryOptions, contextPropagators);
    }

    public LocalActivityOptions validateAndBuildWithDefaults() {
      RetryOptions ro = null;
      if (retryOptions != null) {
        ro = new RetryOptions.Builder(retryOptions).validateBuildWithDefaults();
      }
      return new LocalActivityOptions(
          roundUpToSeconds(scheduleToCloseTimeout), ro, contextPropagators);
    }
  }

  private final Duration scheduleToCloseTimeout;
  private final RetryOptions retryOptions;
  private final List<ContextPropagator> contextPropagators;

  private LocalActivityOptions(
      Duration scheduleToCloseTimeout,
      RetryOptions retryOptions,
      List<ContextPropagator> contextPropagators) {
    this.scheduleToCloseTimeout = scheduleToCloseTimeout;
    this.retryOptions = retryOptions;
    this.contextPropagators = contextPropagators;
  }

  public Duration getScheduleToCloseTimeout() {
    return scheduleToCloseTimeout;
  }

  public RetryOptions getRetryOptions() {
    return retryOptions;
  }

  public List<ContextPropagator> getContextPropagators() {
    return contextPropagators;
  }

  @Override
  public String toString() {
    return "LocalActivityOptions{"
        + "scheduleToCloseTimeout="
        + scheduleToCloseTimeout
        + ", retryOptions="
        + retryOptions
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    LocalActivityOptions that = (LocalActivityOptions) o;
    return Objects.equals(scheduleToCloseTimeout, that.scheduleToCloseTimeout)
        && Objects.equals(retryOptions, that.retryOptions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(scheduleToCloseTimeout, retryOptions);
  }
}
