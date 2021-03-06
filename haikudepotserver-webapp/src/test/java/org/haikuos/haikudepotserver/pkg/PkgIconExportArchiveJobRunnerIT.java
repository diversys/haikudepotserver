/*
 * Copyright 2015, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

package org.haikuos.haikudepotserver.pkg;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.common.io.ByteSource;
import junit.framework.Assert;
import org.fest.assertions.Assertions;
import org.haikuos.haikudepotserver.AbstractIntegrationTest;
import org.haikuos.haikudepotserver.IntegrationTestSupportService;
import org.haikuos.haikudepotserver.WrapWithNoCloseInputStream;
import org.haikuos.haikudepotserver.job.JobOrchestrationService;
import org.haikuos.haikudepotserver.job.model.JobDataWithByteSource;
import org.haikuos.haikudepotserver.job.model.JobSnapshot;
import org.haikuos.haikudepotserver.pkg.model.PkgIconExportArchiveJobSpecification;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@ContextConfiguration({
        "classpath:/spring/servlet-context.xml",
        "classpath:/spring/test-context.xml"
})
public class PkgIconExportArchiveJobRunnerIT extends AbstractIntegrationTest {

    private static Logger LOGGER = LoggerFactory.getLogger(PkgIconExportArchiveJobRunnerIT.class);

    @Resource
    private IntegrationTestSupportService integrationTestSupportService;

    @Resource
    private JobOrchestrationService jobOrchestrationService;


    /**
     * <p>Uses the sample data and checks that the output from the report matches a captured, sensible-looking
     * previous run.</p>
     */

    @Test
    public void testRun() throws IOException {

        integrationTestSupportService.createStandardTestData(); // pkg1 has some icons

        // ------------------------------------
        Optional<String> guidOptional = jobOrchestrationService.submit(
                new PkgIconExportArchiveJobSpecification(),
                JobOrchestrationService.CoalesceMode.NONE);
        // ------------------------------------

        jobOrchestrationService.awaitJobConcludedUninterruptibly(guidOptional.get(), 10000);
        Optional<? extends JobSnapshot> snapshotOptional = jobOrchestrationService.tryGetJob(guidOptional.get());
        Assert.assertEquals(snapshotOptional.get().getStatus(), JobSnapshot.Status.FINISHED);

        // pull in the ZIP file now and extract the icons.

        String dataGuid = snapshotOptional.get().getGeneratedDataGuids().iterator().next();
        JobDataWithByteSource jobSource = jobOrchestrationService.tryObtainData(dataGuid).get();

        try (
                InputStream inputStream = jobSource.getByteSource().openBufferedStream();
                final ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        ) {

            ZipEntry zipEntry;
            Pattern pngPattern = Pattern.compile("hds_pkgiconexportarchive/pkg1/([0-9]+).png");
            ByteSource zipNoCloseInputStreamByteSource = new ByteSource() {
                @Override
                public InputStream openStream() throws IOException {
                    return new WrapWithNoCloseInputStream(zipInputStream);
                }
            };

            Set<String> foundPkg1Filenames = Sets.newHashSet();

            while(null != (zipEntry = zipInputStream.getNextEntry())) {

                if(zipEntry.getName().contains("/pkg1/")) {
                    if (zipEntry.getName().endsWith("/pkg1/icon.hvif")) {
                        getResourceByteSource("/sample.hvif").contentEquals(zipNoCloseInputStreamByteSource);
                        foundPkg1Filenames.add("icon.hvif");
                    }
                    else {

                        Matcher matcher = pngPattern.matcher(zipEntry.getName());

                        if (matcher.matches()) {
                            String expectedPath = "/sample-" + matcher.group(1) + "x" + matcher.group(1) + ".png";
                            getResourceByteSource(expectedPath).contentEquals(zipNoCloseInputStreamByteSource);
                            foundPkg1Filenames.add(matcher.group(1) + ".png");
                        }
                        else {
                            Assert.fail("the zip entry has an unknown file; " + zipEntry.getName());
                        }
                    }
                }
                else {
                    LOGGER.info("ignoring; {}", zipEntry.getName());
                }
            }

            Assertions.assertThat(foundPkg1Filenames).contains(
                    "16.png",
                    "32.png",
                    "icon.hvif");

        }

    }

}
