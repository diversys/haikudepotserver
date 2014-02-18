/*
 * Copyright 2014, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

package org.haikuos.haikudepotserver.api1.model.pkg;

import java.util.List;

public class GetPkgScreenshotsResult {

    public List<PkgScreenshot> items;

    public static class PkgScreenshot {
        public String code;
        public Integer length;
        public Integer height;
        public Integer width;
    }

}