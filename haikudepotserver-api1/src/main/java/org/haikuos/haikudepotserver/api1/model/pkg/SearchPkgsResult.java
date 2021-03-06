/*
 * Copyright 2013-2015, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

package org.haikuos.haikudepotserver.api1.model.pkg;

import org.haikuos.haikudepotserver.api1.support.AbstractSearchResult;

import java.util.List;

public class SearchPkgsResult extends AbstractSearchResult<SearchPkgsResult.Pkg> {

    public static class Pkg {

        public String name;

        /**
         * <p>This value is localized.</p>
         * @since 2015-02-26
         */

        public String title;

        public Long modifyTimestamp;

        /**
         * <p>This will be true if the package has any icon data stored for it;
         * regardless of the format of that icon.  This can be used as a cue as
         * to the value in using a specific URL to get a specific icon for the
         * package or to use a generic icon.</p>
         */

        public boolean hasAnyPkgIcons;

        /**
         * <p>This versions value should only contain the one item actually, but is
         * provided in this form to retain consistency with other API.</p>
         */

        public List<PkgVersion> versions;
        public Float derivedRating;
    }

    public static class PkgVersion {
        public String major;
        public String minor;
        public String micro;
        public String preRelease;
        public Integer revision;
        public Long createTimestamp;
        public Long viewCounter;
        public String architectureCode;
        public String summary;
        public Long payloadLength;
    }

}
