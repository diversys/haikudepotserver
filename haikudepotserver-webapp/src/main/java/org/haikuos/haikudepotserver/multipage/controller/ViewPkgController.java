/*
 * Copyright 2014-2015, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

package org.haikuos.haikudepotserver.multipage.controller;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.haikuos.haikudepotserver.dataobjects.Architecture;
import org.haikuos.haikudepotserver.dataobjects.NaturalLanguage;
import org.haikuos.haikudepotserver.dataobjects.Pkg;
import org.haikuos.haikudepotserver.dataobjects.PkgVersion;
import org.haikuos.haikudepotserver.multipage.MultipageConstants;
import org.haikuos.haikudepotserver.support.web.NaturalLanguageWebHelper;
import org.haikuos.haikudepotserver.multipage.MultipageObjectNotFoundException;
import org.haikuos.haikudepotserver.support.VersionCoordinates;
import org.haikuos.haikudepotserver.support.web.WebConstants;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * <p>'Page' for showing a version of a package.</p>
 */

@Controller
@RequestMapping(MultipageConstants.PATH_MULTIPAGE + "/pkg")
public class ViewPkgController {

    @Resource
    private ServerRuntime serverRuntime;

    private String hyphenToNull(String part) {
        if(null!=part && !part.equals("-")) {
            return part;
        }

        return null;
    }

    @RequestMapping(value = "{name}/{major}/{minor}/{micro}/{preRelease}/{revision}/{architectureCode}", method = RequestMethod.GET)
    public ModelAndView viewPkg(
            HttpServletRequest httpServletRequest,
            @PathVariable(value="name") String pkgName,
            @PathVariable(value="major") String major,
            @PathVariable(value="minor") String minor,
            @PathVariable(value="micro") String micro,
            @PathVariable(value="preRelease") String preRelease,
            @PathVariable(value="revision") String revisionStr,
            @PathVariable(value="architectureCode") String architectureCode) throws MultipageObjectNotFoundException {

        major = hyphenToNull(major);
        minor = hyphenToNull(minor);
        micro = hyphenToNull(micro);
        preRelease = hyphenToNull(preRelease);
        revisionStr = hyphenToNull(revisionStr);

        Integer revision = null==revisionStr ? null : Integer.parseInt(revisionStr);

        ObjectContext context = serverRuntime.getContext();
        Optional<Pkg> pkgOptional = Pkg.getByName(context, pkgName);

        if(!pkgOptional.isPresent()) {
            throw new MultipageObjectNotFoundException(Pkg.class.getSimpleName(), pkgName); // 404
        }

        Optional<Architecture> architectureOptional = Architecture.getByCode(context, architectureCode);

        if(!architectureOptional.isPresent()) {
            throw new MultipageObjectNotFoundException(Architecture.class.getSimpleName(), architectureCode);
        }

        VersionCoordinates coordinates = new VersionCoordinates(
                Strings.emptyToNull(major),
                Strings.emptyToNull(minor),
                Strings.emptyToNull(micro),
                Strings.emptyToNull(preRelease),
                revision);

        Optional<PkgVersion> pkgVersionOptional = PkgVersion.getForPkg(
                context,
                pkgOptional.get(),
                architectureOptional.get(),
                coordinates);

        if(!pkgVersionOptional.isPresent()) {
            throw new MultipageObjectNotFoundException(PkgVersion.class.getSimpleName(), pkgName + "...");
        }

        String homeUrl;

        {
            UriComponentsBuilder builder = UriComponentsBuilder.fromPath(MultipageConstants.PATH_MULTIPAGE);
            String naturalLanguageCode = httpServletRequest.getParameter(WebConstants.KEY_NATURALLANGUAGECODE);

            if(!Strings.isNullOrEmpty(naturalLanguageCode)) {
                builder.queryParam(WebConstants.KEY_NATURALLANGUAGECODE, naturalLanguageCode);
            }

            homeUrl = builder.build().toString();
        }

        ViewPkgVersionData data = new ViewPkgVersionData();

        data.setPkgVersion(pkgVersionOptional.get());
        data.setCurrentNaturalLanguage(NaturalLanguageWebHelper.deriveNaturalLanguage(context, httpServletRequest));
        data.setHomeUrl(homeUrl);

        ModelAndView result = new ModelAndView("multipage/viewPkgVersion");
        result.addObject("data", data);

        return result;

    }


    /**
     * <p>This is the data model for the page to be rendered from.</p>
     */

    public static class ViewPkgVersionData {

        private PkgVersion pkgVersion;

        private NaturalLanguage currentNaturalLanguage;

        private String homeUrl;

        public PkgVersion getPkgVersion() {
            return pkgVersion;
        }

        public void setPkgVersion(PkgVersion pkgVersion) {
            this.pkgVersion = pkgVersion;
        }

        public NaturalLanguage getCurrentNaturalLanguage() {
            return currentNaturalLanguage;
        }

        public void setCurrentNaturalLanguage(NaturalLanguage currentNaturalLanguage) {
            this.currentNaturalLanguage = currentNaturalLanguage;
        }

        public String getHomeUrl() {
            return homeUrl;
        }

        public void setHomeUrl(String homeUrl) {
            this.homeUrl = homeUrl;
        }
    }


}