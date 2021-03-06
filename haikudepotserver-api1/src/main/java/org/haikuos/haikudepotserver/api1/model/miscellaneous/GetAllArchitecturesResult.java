/*
 * Copyright 2013, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

package org.haikuos.haikudepotserver.api1.model.miscellaneous;

import java.util.List;

public class GetAllArchitecturesResult {

    public List<Architecture> architectures;

    public static class Architecture {
        public String code;
    }

}
