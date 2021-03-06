/*
 * Copyright 2014, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

package org.haikuos.haikudepotserver.api1.model.repository;

import java.util.List;

public class UpdateRepositoryRequest {

    public enum Filter {
        ACTIVE,
        URL
    };

    public String code;

    public Boolean active;

    public String url;

    public List<Filter> filter;

}
