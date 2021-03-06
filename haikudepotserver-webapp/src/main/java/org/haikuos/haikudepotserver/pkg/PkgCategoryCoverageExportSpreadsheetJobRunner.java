/*
 * Copyright 2014-2015, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

package org.haikuos.haikudepotserver.pkg;

import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.net.MediaType;
import org.apache.cayenne.ObjectContext;
import org.haikuos.haikudepotserver.dataobjects.Pkg;
import org.haikuos.haikudepotserver.dataobjects.PkgVersionLocalization;
import org.haikuos.haikudepotserver.job.JobOrchestrationService;
import org.haikuos.haikudepotserver.job.model.JobDataWithByteSink;
import org.haikuos.haikudepotserver.job.model.JobRunnerException;
import org.haikuos.haikudepotserver.pkg.model.PkgCategoryCoverageExportSpreadsheetJobSpecification;
import org.haikuos.haikudepotserver.support.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.List;

/**
 * <p>This report is a spreadsheet that covers basic details of each package.</p>
 */

@Component
public class PkgCategoryCoverageExportSpreadsheetJobRunner extends AbstractPkgCategorySpreadsheetJobRunner<PkgCategoryCoverageExportSpreadsheetJobSpecification> {

    private static Logger LOGGER = LoggerFactory.getLogger(PkgCategoryCoverageExportSpreadsheetJobRunner.class);

    @Override
    public void run(
            JobOrchestrationService jobOrchestrationService,
            PkgCategoryCoverageExportSpreadsheetJobSpecification specification)
            throws IOException, JobRunnerException {

        Preconditions.checkArgument(null!=jobOrchestrationService);
        assert null!=jobOrchestrationService;
        Preconditions.checkArgument(null!=specification);

        final ObjectContext context = serverRuntime.getContext();

        // this will register the outbound data against the job.
        JobDataWithByteSink jobDataWithByteSink = jobOrchestrationService.storeGeneratedData(
                specification.getGuid(),
                "download",
                MediaType.CSV_UTF_8.toString());

        try(
                OutputStream outputStream = jobDataWithByteSink.getByteSink().openBufferedStream();
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
                CSVWriter writer = new CSVWriter(outputStreamWriter, ',')
        ) {

            // headers

            final List<String> pkgCategoryCodes = getPkgCategoryCodes();

            List<String> headings = Lists.newArrayList();
            headings.add("pkg-name");
            headings.add("any-summary");
            headings.add("none");
            Collections.addAll(headings, pkgCategoryCodes.toArray(new String[pkgCategoryCodes.size()]));
            headings.add("action");

            long startMs = System.currentTimeMillis();

            writer.writeNext(headings.toArray(new String[headings.size()]));

            // stream out the packages.

            LOGGER.info("will produce category coverage spreadsheet report");


            long count = pkgOrchestrationService.eachPkg(
                    context,
                    false,
                    new Callback<Pkg>() {
                        @Override
                        public boolean process(Pkg pkg) {
                            List<String> cols = Lists.newArrayList();
                            Optional<PkgVersionLocalization> locOptional = Optional.absent();

                            if(null!=pkg) {
                                locOptional = PkgVersionLocalization.getAnyPkgVersionLocalizationForPkg(context, pkg);
                            }

                            cols.add(pkg.getName());
                            cols.add(locOptional.isPresent() ? locOptional.get().getSummary() : "");
                            cols.add(pkg.getPkgPkgCategories().isEmpty() ? MARKER : "");

                            for (String pkgCategoryCode : pkgCategoryCodes) {
                                cols.add(pkg.getPkgPkgCategory(pkgCategoryCode).isPresent() ? MARKER : "");
                            }

                            cols.add(""); // no action
                            writer.writeNext(cols.toArray(new String[cols.size()]));
                            return true; // keep going!
                        }
                    }
            );

            LOGGER.info(
                    "did produce category coverage spreadsheet report for {} packages in {}ms",
                    count,
                    System.currentTimeMillis() - startMs);

        }

    }

}
