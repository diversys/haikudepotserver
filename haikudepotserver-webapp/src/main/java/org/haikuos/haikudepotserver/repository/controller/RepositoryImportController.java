/*
 * Copyright 2013-2014, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

package org.haikuos.haikudepotserver.repository.controller;

import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import org.haikuos.haikudepotserver.repository.model.PkgRepositoryImportJobSpecification;
import org.haikuos.haikudepotserver.job.JobOrchestrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * <p>This is the HTTP endpoint from which external systems are able to trigger a repository to be scanned for
 * new packages by fetching the HPKR file and processing it.  The actual logistics in this controller do not use
 * typical Spring MVC error handling and so on; this is because fine control is required and this seems to be
 * an easy way to achieve that; basically done manually.</p>
 */

@Controller
@RequestMapping("/importrepositorydata")
public class RepositoryImportController {

    protected static Logger LOGGER = LoggerFactory.getLogger(RepositoryImportController.class);

    public final static String KEY_CODE = "code";

    @Resource
    private JobOrchestrationService jobOrchestrationService;

    @RequestMapping(method = RequestMethod.GET)
    public void fetch(
            HttpServletResponse response,
            @RequestParam(value = KEY_CODE, required = false) String repositoryCode) {

        try {
            if(Strings.isNullOrEmpty(repositoryCode)) {
                LOGGER.warn("attempt to import repository data service with no repository code supplied");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.PLAIN_TEXT_UTF_8.toString());
                response.getWriter().print(String.format("expected '%s' to have been a query argument to this resource\n",KEY_CODE));
            }
            else {
                jobOrchestrationService.submit(
                        new PkgRepositoryImportJobSpecification(repositoryCode),
                        JobOrchestrationService.CoalesceMode.QUEUED);

                response.setStatus(HttpServletResponse.SC_OK);
                response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.PLAIN_TEXT_UTF_8.toString());
                response.getWriter().print(String.format("accepted import repository job for repository %s\n",repositoryCode));
            }
        }
        catch(Throwable th) {
            LOGGER.error("failed to accept import repository job", th);

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.PLAIN_TEXT_UTF_8.toString());

            try {
                response.getWriter().print(String.format("failed to accept import repository job for repository %s\n",repositoryCode));
            }
            catch(IOException ioe) {
                /* ignore */
            }
        }
    }

}
