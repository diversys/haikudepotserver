/*
 * Copyright 2014, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

package org.haikuos.haikudepotserver.job;

import org.haikuos.haikudepotserver.job.model.JobSpecification;
import org.haikuos.haikudepotserver.job.model.JobRunnerException;

import java.io.IOException;

/**
 * <p>This is a re-entrant object that is able to run a job.  It knows the type of the report that it is able to run
 * and is also able to be invoked to run the job.  Concrete implementations exist in the application context and are
 * accessed by the {@link org.haikuos.haikudepotserver.job.JobOrchestrationService}.</p>
 */

public interface JobRunner<T extends JobSpecification> {

    /**
     * <p>This string defines the type of the job.  It is used to identify a runner to run a job specification
     * in the {@link org.haikuos.haikudepotserver.job.JobOrchestrationService}.</p>
     */

    String getJobTypeCode();

    void run(JobOrchestrationService jobOrchestrationService, T specification) throws IOException, JobRunnerException;

}
