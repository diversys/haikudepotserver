/*
 * Copyright 2013-2015, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

package org.haikuos.haikudepotserver.dataobjects;

import com.google.common.base.*;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.validation.BeanValidationFailure;
import org.apache.cayenne.validation.ValidationResult;
import org.haikuos.haikudepotserver.dataobjects.auto._PkgVersion;
import org.haikuos.haikudepotserver.dataobjects.support.CreateAndModifyTimestamped;
import org.haikuos.haikudepotserver.support.VersionCoordinates;
import org.haikuos.haikudepotserver.support.cayenne.ExpressionHelper;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;

public class PkgVersion extends _PkgVersion implements CreateAndModifyTimestamped {

    public final static Pattern MAJOR_PATTERN = Pattern.compile("^[\\w_]+$");
    public final static Pattern MINOR_PATTERN = Pattern.compile("^[\\w_]+$");
    public final static Pattern MICRO_PATTERN = Pattern.compile("^[\\w_.]+$");
    public final static Pattern PRE_RELEASE_PATTERN = Pattern.compile("^[\\w_.]+$");

    public static PkgVersion get(ObjectContext context, ObjectId objectId) {
        return Iterables.getOnlyElement((List<PkgVersion>) context.performQuery(new ObjectIdQuery(objectId)));
    }

    public static List<PkgVersion> getForPkg(
            ObjectContext context,
            Pkg pkg) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(pkg);

        SelectQuery query = new SelectQuery(
                PkgVersion.class,
                ExpressionFactory.matchExp(PkgVersion.PKG_PROPERTY, pkg).andExp(
                        ExpressionFactory.matchExp(PkgVersion.ACTIVE_PROPERTY, Boolean.TRUE))
        );

        //noinspection unchecked
        return (List<PkgVersion>) context.performQuery(query);
    }

    public static Optional<PkgVersion> getForPkg(
            ObjectContext context,
            Pkg pkg,
            Architecture architecture,
            VersionCoordinates versionCoordinates) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(pkg);
        Preconditions.checkNotNull(architecture);
        Preconditions.checkNotNull(versionCoordinates);

        SelectQuery query = new SelectQuery(
                PkgVersion.class,
                ExpressionFactory.matchExp(PkgVersion.PKG_PROPERTY, pkg).andExp(
                        ExpressionFactory.matchExp(PkgVersion.ACTIVE_PROPERTY, Boolean.TRUE)).andExp(
                        ExpressionFactory.matchExp(PkgVersion.ARCHITECTURE_PROPERTY, architecture)).andExp(
                        ExpressionHelper.toExpression(versionCoordinates))
        );

        //noinspection unchecked
        return Optional.fromNullable(Iterables.getOnlyElement(
                (List<PkgVersion>) context.performQuery(query),
                null));
    }

    @Override
    public void validateForInsert(ValidationResult validationResult) {

        if(null==getActive()) {
            setActive(true);
        }

        if(null==getViewCounter()) {
            setViewCounter(0l);
        }

        if(null==getIsLatest()) {
            setIsLatest(false);
        }

        super.validateForInsert(validationResult);
    }

    @Override
    protected void validateForSave(ValidationResult validationResult) {
        super.validateForSave(validationResult);

        if(null != getMajor()) {
            if(!MAJOR_PATTERN.matcher(getMajor()).matches()) {
                validationResult.addFailure(new BeanValidationFailure(this,MAJOR_PROPERTY,"malformed"));
            }
        }

        if(null != getMinor()) {
            if(!MINOR_PATTERN.matcher(getMinor()).matches()) {
                validationResult.addFailure(new BeanValidationFailure(this,MINOR_PROPERTY,"malformed"));
            }
        }

        if(null != getMicro()) {
            if(!MICRO_PATTERN.matcher(getMicro()).matches()) {
                validationResult.addFailure(new BeanValidationFailure(this,MICRO_PROPERTY,"malformed"));
            }
        }

        if(null != getPreRelease()) {
            if(!PRE_RELEASE_PATTERN.matcher(getPreRelease()).matches()) {
                validationResult.addFailure(new BeanValidationFailure(this,PRE_RELEASE_PROPERTY,"malformed"));
            }
        }

        if(null != getRevision()) {
            if(getRevision() <= 0) {
                validationResult.addFailure(new BeanValidationFailure(this,REVISION_PROPERTY,"lessThanEqualZero"));
            }
        }

        if(getViewCounter() < 0) {
            validationResult.addFailure(new BeanValidationFailure(this,VIEW_COUNTER_PROPERTY,"min"));
        }

    }

    public void incrementViewCounter() {
        setViewCounter(getViewCounter()+1);
    }

    /**
     * <p>This will try to find localized data for the pkg version for the supplied natural language.  Because
     * English language data is hard-coded into the package payload, english will always be available.</p>
     */

    public Optional<PkgVersionLocalization> getPkgVersionLocalization(final NaturalLanguage naturalLanguage) {
        return getPkgVersionLocalization(naturalLanguage.getCode());
    }

    public PkgVersionLocalization getPkgVersionLocalizationOrFallback(final NaturalLanguage naturalLanguage) {
        return getPkgVersionLocalizationOrFallbackByCode(naturalLanguage.getCode());
    }

    /**
     * <p>This will try to find localized data for the pkg version for the supplied natural language.  Because
     * English language data is hard-coded into the package payload, english will always be available.</p>
     */

    public Optional<PkgVersionLocalization> getPkgVersionLocalization(final String naturalLanguageCode) {
        Preconditions.checkState(!Strings.isNullOrEmpty(naturalLanguageCode));

        return Iterables.tryFind(
                getPkgVersionLocalizations(),
                new Predicate<PkgVersionLocalization>() {
                    @Override
                    public boolean apply(PkgVersionLocalization input) {
                        return input.getNaturalLanguage().getCode().equals(naturalLanguageCode);
                    }
                }
        );
    }

    /**
     * <p>This method will return the localization for the language specified or will return the localization
     * for english if the desired language is not available.</p>
     */

    // have to add the 'byCode' for JSP to be able to work with it.
    public PkgVersionLocalization getPkgVersionLocalizationOrFallbackByCode(final String naturalLanguageCode) {
        Optional<PkgVersionLocalization> pkgVersionLocalizationOptional = Optional.absent();

        if(!Strings.isNullOrEmpty(naturalLanguageCode)) {
            pkgVersionLocalizationOptional = getPkgVersionLocalization(naturalLanguageCode);
        }

        if(!pkgVersionLocalizationOptional.isPresent()) {
            pkgVersionLocalizationOptional = getPkgVersionLocalization(NaturalLanguage.CODE_ENGLISH);
        }

        if(!pkgVersionLocalizationOptional.isPresent()) {
            throw new IllegalStateException("unable to find the fallback localization for " + toString());
        }

        return pkgVersionLocalizationOptional.get();
    }

    /**
     * <p>Renders the copyright entities into a list of strings.</p>
     */

    public List<String> getCopyrights() {
        return Lists.transform(
                getPkgVersionCopyrights(),
                new Function<PkgVersionCopyright, String>() {
                    @Override
                    public String apply(PkgVersionCopyright input) {
                        return input.getBody();
                    }
                }
        );
    }

    /**
     * <p>Renders the licenses entities into a list of strings.</p>
     */

    public List<String> getLicenses() {
        return Lists.transform(
                getPkgVersionLicenses(),
                new Function<PkgVersionLicense, String>() {
                    @Override
                    public String apply(PkgVersionLicense input) {
                        return input.getBody();
                    }
                }
        );
    }

    public Optional<PkgVersionUrl> getPkgVersionUrlForType(final PkgUrlType type) {
        Preconditions.checkNotNull(type);
        return Iterables.tryFind(
                getPkgVersionUrls(),
                new Predicate<PkgVersionUrl>() {
                    @Override
                    public boolean apply(PkgVersionUrl input) {
                        return input.getPkgUrlType().equals(type);
                    }
                }
        );
    }

    /**
     * <p>This method will provide a URL to the actual data of the package.</p>
     * @return
     */

    public URL getHpkgURL() {
        URL packagesBaseUrl = getRepository().getPackagesBaseURL();

        try {
            return new URL(
                    packagesBaseUrl.getProtocol(),
                    packagesBaseUrl.getHost(),
                    packagesBaseUrl.getPort(),
                    packagesBaseUrl.getPath() + "/" + getHpkgFilename());
        }
        catch(MalformedURLException mue) {
            throw new IllegalStateException("unable to create the URL to the hpkg data", mue);
        }
    }

    /**
     * <p>Ultimately, the HPKG file will exist on a server somewhere in the repository for access
     * by clients.  The file must have a name which is able to be derived from information about
     * the package version.  This method will provide that package version's filename for the
     * package file.</p>
     */

    public String getHpkgFilename() {

        StringBuilder builder = new StringBuilder();
        builder.append(getPkg().getName());
        builder.append('-');
        builder.append(getMajor());

        if(null!=getMinor()) {
            builder.append('.');
            builder.append(getMinor());
        }

        if(null!=getMicro()) {
            builder.append('.');
            builder.append(getMicro());
        }

        if(null!=getPreRelease()) {
            builder.append('~');
            builder.append(getPreRelease());
        }

        if(null!=getRevision()) {
            builder.append('-');
            builder.append(getRevision());
        }

        builder.append('-');
        builder.append(getArchitecture().getCode());

        builder.append(".hpkg");

        return builder.toString();
    }

    public VersionCoordinates toVersionCoordinates() {
        return new VersionCoordinates(
                getMajor(),
                getMinor(),
                getMicro(),
                getPreRelease(),
                getRevision());
    }

    public String toStringWithPkgAndArchitecture() {
        return getPkg().getName() + " - " + toString() + " - " + getArchitecture().getCode();
    }

    @Override
    public String toString() {
        return toVersionCoordinates().toString();
    }

}
