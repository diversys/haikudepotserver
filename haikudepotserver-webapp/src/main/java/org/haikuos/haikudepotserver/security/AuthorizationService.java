/*
 * Copyright 2014-2015, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

package org.haikuos.haikudepotserver.security;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.ObjectContext;
import org.haikuos.haikudepotserver.dataobjects.Pkg;
import org.haikuos.haikudepotserver.dataobjects.Repository;
import org.haikuos.haikudepotserver.dataobjects.User;
import org.haikuos.haikudepotserver.dataobjects.UserRating;
import org.haikuos.haikudepotserver.security.model.AuthorizationPkgRule;
import org.haikuos.haikudepotserver.security.model.Permission;
import org.haikuos.haikudepotserver.security.model.TargetType;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <P>This class will provide functions around authorization.  Some of the model for this is provided
 * by the API objects.</p>
 */

@Service
public class AuthorizationService {

    private TargetType deriveTargetType(DataObject dataObject) {
        if(null==dataObject)
            return null;

        if(User.class.isAssignableFrom(dataObject.getClass())) {
            return TargetType.USER;
        }

        if(Pkg.class.isAssignableFrom(dataObject.getClass())) {
            return TargetType.PKG;
        }

        if(Repository.class.isAssignableFrom(dataObject.getClass())) {
            return TargetType.REPOSITORY;
        }

        if(UserRating.class.isAssignableFrom(dataObject.getClass())) {
            return TargetType.USERRATING;
        }

        throw new IllegalStateException("the data object type '"+dataObject.getClass().getSimpleName()+"' is not handled");
    }

    /**
     * <p>Returns true if the user supplied has the permission over the target object.</p>
     */

    public boolean check(
            ObjectContext objectContext,
            User authenticatedUser,
            TargetType targetType,
            String targetIdentifier,
            Permission permission) {
        Preconditions.checkNotNull(permission);
        Preconditions.checkNotNull(objectContext);

        DataObject target = null;

        if(null!=targetType) {

            Optional<? extends DataObject> targetOptional;

            if(Strings.isNullOrEmpty(targetIdentifier)) {
                throw new IllegalStateException("the target type is supplied, but no target identifier");
            }

            switch(targetType) {
                case PKG:
                    targetOptional = Pkg.getByName(objectContext, targetIdentifier);
                    break;

                case REPOSITORY:
                    targetOptional = Repository.getByCode(objectContext, targetIdentifier);
                    break;

                case USER:
                    targetOptional = User.getByNickname(objectContext, targetIdentifier);
                    break;

                case USERRATING:
                    targetOptional = UserRating.getByCode(objectContext, targetIdentifier);
                    break;

                default:
                    throw new IllegalStateException("the target type is not handled; "+targetType.name());
            }

            // if the object was not able to be found then we should bail-out and say that the permission
            // does not apply.

            if(!targetOptional.isPresent()) {
                return false;
            }

            target = targetOptional.get();
        }
        else {
            if(!Strings.isNullOrEmpty(targetIdentifier)) {
                throw new IllegalStateException("the target type was supplied, but not the target identifier");
            }
        }

        return check(objectContext, authenticatedUser, target, permission);
    }

    /**
     * <p>This method will return true if the permission applies in this situation.</p>
     */

    public boolean check(
            ObjectContext objectContext,
            User authenticatedUser,
            DataObject target,
            final Permission permission) {

        Preconditions.checkNotNull(permission);
        Preconditions.checkNotNull(objectContext);
        Preconditions.checkState(
                deriveTargetType(target) == permission.getRequiredTargetType(),
                "during checking authorization, the target object type " + deriveTargetType(target) + " does not match the expected type "  + permission.getRequiredTargetType());

        // if the authenticated user is not active then there should not be a situation arising where
        // an authorization check is being made.

        if(null!=authenticatedUser && !authenticatedUser.getActive()) {
            throw new IllegalStateException("the authenticated user '"+authenticatedUser.getNickname()+"' is not active and so authorization queries cannot be resolved for them");
        }

        // it could be that permission is afforded based on rules stored in the user.  Check for
        // this situation first.

        if(null!=authenticatedUser) {
            switch (permission) {
                case PKG_EDITICON:
                case PKG_EDITSCREENSHOT:
                case PKG_EDITCATEGORIES:
                case PKG_EDITPROMINENCE:
                case PKG_EDITLOCALIZATION:
                case PKG_EDITVERSIONLOCALIZATION: {
                    List<? extends AuthorizationPkgRule> rules = authenticatedUser.getAuthorizationPkgRules((Pkg) target);
                    if (Iterables.tryFind(rules, new Predicate<AuthorizationPkgRule>() {
                        @Override
                        public boolean apply(AuthorizationPkgRule input) {
                            return input.getPermission().getCode().equalsIgnoreCase(permission.name());
                        }
                    }).isPresent()) {
                        return true;
                    }
                }
                break;
            }
        }

        // fall back to application-logic rules.

        switch(permission) {

            case AUTHORIZATION_CONFIGURE:
                return null!=authenticatedUser && authenticatedUser.getIsRoot();

            case REPOSITORY_EDIT:
            case REPOSITORY_IMPORT:
                return null!=authenticatedUser && authenticatedUser.getIsRoot();

            case REPOSITORY_VIEW:
                Repository repository = (Repository) target;
                return repository.getActive() || (null!=authenticatedUser && authenticatedUser.getIsRoot());

            case REPOSITORY_LIST:
                return true;

            case REPOSITORY_LIST_INACTIVE:
                return null!=authenticatedUser && authenticatedUser.getIsRoot();

            case REPOSITORY_ADD:
                return null!=authenticatedUser && authenticatedUser.getIsRoot();

            case USER_VIEWJOBS:
            case USER_VIEW:
            case USER_EDIT:
            case USER_CHANGEPASSWORD:
                return
                        null!=authenticatedUser
                                && (authenticatedUser.getIsRoot() || authenticatedUser.equals(target));

            case USER_SYNCHRONIZE:
            case USER_LIST:
                return null!=authenticatedUser && authenticatedUser.getIsRoot();

            case PKG_CREATEUSERRATING:
                return true;

            case PKG_EDITICON:
            case PKG_EDITSCREENSHOT:
            case PKG_EDITCATEGORIES:
            case PKG_EDITLOCALIZATION:
            case PKG_EDITVERSIONLOCALIZATION:
            case PKG_EDITPROMINENCE:
                return null!=authenticatedUser && authenticatedUser.getIsRoot();

            case USERRATING_EDIT:
                UserRating userRating = (UserRating) target;
                return null!=authenticatedUser && (userRating.getUser().equals(authenticatedUser) || authenticatedUser.getIsRoot());

            case USERRATING_DERIVEANDSTOREFORPKG:
                return null!=authenticatedUser && authenticatedUser.getIsRoot();

            case BULK_PKGVERSIONLOCALIZATIONCOVERAGEEXPORTSPREADSHEET:
            case BULK_PKGPROMINENCEANDUSERRATINGSPREADSHEETREPORT:
            case BULK_PKGICONSPREADSHEETREPORT:
            case BULK_PKGCATEGORYCOVERAGEEXPORTSPREADSHEET:
            case BULK_PKGICONEXPORTARCHIVE:
                return null!=authenticatedUser;

            case BULK_PKGCATEGORYCOVERAGEIMPORTSPREADSHEET:
                return null!=authenticatedUser && authenticatedUser.getIsRoot();

            case BULK_USERRATINGSPREADSHEETREPORT_ALL:
                return null!=authenticatedUser && authenticatedUser.getIsRoot();

            case BULK_USERRATINGSPREADSHEETREPORT_PKG:
                return null!=authenticatedUser;

            case BULK_USERRATINGSPREADSHEETREPORT_USER:
                return null!=authenticatedUser &&
                        (authenticatedUser.getIsRoot() ||
                                authenticatedUser.getNickname().equals(((User) target).getNickname()));

            case BULK_PKGVERSIONPAYLOADLENGTHPOPULATION:
                return null!=authenticatedUser && authenticatedUser.getIsRoot();

            case JOBS_VIEW:
                return null!=authenticatedUser && authenticatedUser.getIsRoot();

            default:
                throw new IllegalStateException("unhandled permission; "+permission.name());
        }

    }

}
