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

package com.uber.cadence.workerFactory;

import static org.junit.Assert.*;

import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.serviceclient.ClientOptions;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.serviceclient.WorkflowServiceTChannel;
import com.uber.cadence.worker.WorkerFactory;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class WorkerFactoryTests {

  private static final boolean useDockerService =
      Boolean.parseBoolean(System.getenv("USE_DOCKER_SERVICE"));

  @BeforeClass
  public static void beforeClass() {
    Assume.assumeTrue(useDockerService);
  }

  private IWorkflowService service;
  private WorkflowClient client;
  private WorkerFactory factory;

  @Before
  public void setUp() {
    service = new WorkflowServiceTChannel(ClientOptions.defaultInstance());
    client = WorkflowClient.newInstance(service);
    factory = WorkerFactory.newInstance(client);
  }

  @After
  public void tearDown() {
    service.close();
  }

  @Test
  public void whenAFactoryIsStartedAllWorkersStart() {
    factory.newWorker("task1");
    factory.newWorker("task2");

    factory.start();
    assertTrue(factory.isStarted());
    try {
      assertTrue(factory.isHealthy().get());
    } catch (Exception e) {
      assertNull("Failed to check if cluster is health!", e);
    }
    factory.shutdown();
    factory.awaitTermination(1, TimeUnit.SECONDS);
  }

  @Test
  public void whenAFactoryIsShutdownAllWorkersAreShutdown() {
    factory.newWorker("task1");
    factory.newWorker("task2");

    assertFalse(factory.isStarted());
    factory.start();
    assertTrue(factory.isStarted());
    assertFalse(factory.isShutdown());
    factory.shutdown();
    factory.awaitTermination(1, TimeUnit.MILLISECONDS);

    assertTrue(factory.isShutdown());
    factory.shutdown();
    assertTrue(factory.isShutdown());
    factory.awaitTermination(1, TimeUnit.SECONDS);
  }

  @Test
  public void aFactoryCanBeStartedMoreThanOnce() {
    factory.start();
    factory.start();
    factory.shutdown();
    factory.awaitTermination(1, TimeUnit.SECONDS);
  }

  @Test(expected = IllegalStateException.class)
  public void aFactoryCannotBeStartedAfterShutdown() {
    factory.newWorker("task1");

    factory.shutdown();
    factory.awaitTermination(1, TimeUnit.MILLISECONDS);
    factory.start();
  }

  @Test(expected = IllegalStateException.class)
  public void workersCannotBeCreatedAfterFactoryHasStarted() {
    factory.newWorker("task1");

    factory.start();

    try {
      factory.newWorker("task2");
    } finally {
      factory.shutdown();
      factory.awaitTermination(1, TimeUnit.SECONDS);
    }
  }

  @Test(expected = IllegalStateException.class)
  public void workersCannotBeCreatedAfterFactoryIsShutdown() {
    factory.newWorker("task1");

    factory.shutdown();
    factory.awaitTermination(1, TimeUnit.MILLISECONDS);
    try {
      factory.newWorker("task2");
    } finally {
      factory.shutdown();
      factory.awaitTermination(1, TimeUnit.SECONDS);
    }
  }

  @Test
  public void factoryCanOnlyBeShutdownMoreThanOnce() {
    factory.newWorker("task1");

    factory.shutdown();
    factory.awaitTermination(1, TimeUnit.MILLISECONDS);
    factory.shutdown();
    factory.awaitTermination(1, TimeUnit.MILLISECONDS);
  }
}
