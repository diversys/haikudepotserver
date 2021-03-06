/*
 * Copyright 2014, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

package org.haikuos.haikudepotserver.pkg.model;

import org.haikuos.haikudepotserver.job.model.AbstractJobSpecification;
import org.haikuos.haikudepotserver.job.model.JobSpecification;
import org.springframework.util.ObjectUtils;

public class PkgCategoryCoverageExportSpreadsheetJobSpecification extends AbstractJobSpecification {

    @Override
    public boolean isEquivalent(JobSpecification other) {
        return
                PkgCategoryCoverageImportSpreadsheetJobSpecification.class.isAssignableFrom(other.getClass())
                        && ObjectUtils.nullSafeEquals(other.getOwnerUserNickname(), getOwnerUserNickname());
    }

}
