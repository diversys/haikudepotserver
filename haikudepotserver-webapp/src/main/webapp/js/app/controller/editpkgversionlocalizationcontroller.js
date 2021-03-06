/*
 * Copyright 2014-2015, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

/**
 * <p>This controller allows the user to be able to provide translations for the package version.
 * The scope contains a number of 'translations' that couple the natural language together with
 * the data that has been translated.  Only translations for some languages will be shown; those
 * with existing data to start with.</p>
 */

angular.module('haikudepotserver').controller(
    'EditPkgVersionLocalizationController',
    [
        '$scope','$log','$location','$routeParams',
        'jsonRpc','constants','pkgIcon','errorHandling',
        'breadcrumbs','breadcrumbFactory','userState','referenceData','pkg','messageSource',
        function(
            $scope,$log,$location,$routeParams,
            jsonRpc,constants,pkgIcon,errorHandling,
            breadcrumbs,breadcrumbFactory,userState,referenceData,pkg,messageSource) {

            var ARCHITECTUREAPPLICABILITY_ALL = '__all';

            $scope.showHelp = false;
            $scope.pkg = undefined;
            $scope.amSaving = false;
            $scope.translations = undefined;
            $scope.englishTranslation = undefined;
            $scope.architectureCode = $routeParams.architectureCode;
            $scope.selectedArchitectureApplicability = ARCHITECTUREAPPLICABILITY_ALL;
            $scope.originalTranslations = undefined;
            $scope.selectedTranslation = undefined;
            $scope.addableNaturalLanguages = undefined;
            $scope.selectedAddableNaturalLanguage = undefined;
            $scope.editPkgVersionLocalizations = { };

            $scope.shouldSpin = function() {
                return undefined == $scope.pkg || $scope.amSaving || undefined == $scope.translations;
            };

            $scope.isTranslationSelected = function(translation) {
                return $scope.selectedTranslation.naturalLanguage.code == translation.naturalLanguage.code;
            };

            function findOriginalTranslation(naturalLanguageCode) {
                var result = _.find(
                    $scope.originalTranslations,
                    function(t) {
                        return t.naturalLanguage.code == naturalLanguageCode;
                    }
                );

                if(!result) {
                    throw Error('was not able to find the original translation');
                }

                return result;
            }

            /**
             * <p>The only requirement for a translation to be valid is that if the description and the summary
             * are either present or not present.  It is not possible to have a summary and not a description
             * and vica-versa.</p>
             */

            $scope.isTranslationValid = function(translation) {
                if(translation) {
                    var hasSummary = translation.summary && translation.summary.length;
                    var hasDescription = translation.description && translation.description.length;
                    return !!hasDescription == !!hasSummary;
                }

                return true;
            };

            /**
             * <p>This colours the text of the language to indicate if the translation is missing, invalid or
             * has been supplied successfully.</p>
             */

            $scope.classesForTranslation = function(translation) {
                var classes = [];
                var hasSummary = translation.summary && translation.summary.length;
                var hasDescription = translation.description && translation.description.length;

                if(hasDescription && hasSummary) {
                    classes.push('text-success');
                }
                else {
                    if(hasDescription != hasSummary) {
                        classes.push('text-error');
                    }
                    else {
                        classes.push('text-warning');
                    }
                }

                if($scope.isTranslationSelected(translation)) {
                    classes.push('selected-translation');
                }

                return classes;
            };

            $scope.goChooseTranslation = function(translation) {
                if(!translation) {
                    throw Error('the translation must be provided to select');
                }

                $scope.selectedTranslation = translation;
            };

            $scope.goShowHelp = function() {
                $scope.showHelp = true;
            };

            $scope.deriveFormControlsContainerClasses = function(name) {
                return $scope.editPkgIconForm[name].$invalid ? ['form-control-group-error'] : [];
            };

            function refreshBreadcrumbItems() {
                breadcrumbs.mergeCompleteStack([
                    breadcrumbFactory.createHome(),
                    breadcrumbFactory.createViewPkgWithSpecificVersionFromPkg($scope.pkg),
                    breadcrumbFactory.applyCurrentLocation(breadcrumbFactory.createEditPkgVersionLocalization($scope.pkg))
                ]);
            }

            function setupTranslations() {
                referenceData.naturalLanguages().then(
                    function(naturalLanguageData) {

                        // bring in titles for the natural languages.

                        function updateNaturalLanguageOptionsTitles() {
                            _.each(naturalLanguageData, function(nl) {
                                messageSource.get(userState.naturalLanguageCode(), 'naturalLanguage.' + nl.code).then(
                                    function(value) {
                                        nl.title = value;
                                    },
                                    function() {
                                        $log.error('unable to get the localized name for the natural language \''+nl.code+'\'');
                                    }
                                );
                            });
                        }

                        updateNaturalLanguageOptionsTitles();

                        // now we need to get the _existing_ translations for the package.

                        jsonRpc.call(
                            constants.ENDPOINT_API_V1_PKG,
                            'getPkgVersionLocalizations',
                            [{
                                pkgName: $routeParams.name,
                                naturalLanguageCodes : _.map(
                                    naturalLanguageData,
                                    function(d) {
                                        return d.code;
                                    }
                                ),
                                architectureCode : $routeParams.architectureCode
                            }]
                        )
                            .then(
                            function(pkgVersionLocalizationsData) {

                                // now merge the data about the various natural languages together with the data about
                                // the packages existing localizations and we should have enough working data to setup
                                // an internal data model.

                                $scope.translations = _.map(
                                    naturalLanguageData,
                                    function (d) {

                                        var pkgVersionLocalizationData = _.findWhere(
                                            pkgVersionLocalizationsData.pkgVersionLocalizations,
                                            { naturalLanguageCode: d.code }
                                        );

                                        return {
                                            naturalLanguage: d,
                                            summary: pkgVersionLocalizationData ? pkgVersionLocalizationData.summary : '',
                                            description: pkgVersionLocalizationData ? pkgVersionLocalizationData.description : '',
                                            wasEdited: false
                                        };
                                    }
                                );

                                // find the english language translation in order that it can be displayed as
                                // the 'original'.

                                $scope.englishTranslation = _.find(
                                    $scope.translations,
                                    function(t) {
                                        return t.naturalLanguage.code == 'en';
                                    }
                                );

                                $scope.translations = _.filter(
                                    $scope.translations,
                                    function (translation) {

                                        // don't include English as a localization target because the English language will
                                        // have been included in the hpkg data from the repository.

                                        return translation.naturalLanguage.code != constants.NATURALLANGUAGECODE_ENGLISH &&
                                            ( translation.naturalLanguage.hasData ||
                                                translation.naturalLanguage.hasLocalizationMessages ||
                                                (translation.summary && translation.summary.length) ||
                                                (translation.description && translation.description.length) );
                                    }
                                );

                                $scope.originalTranslations = angular.copy($scope.translations);
                                $scope.selectedTranslation = $scope.translations[0];

                                $log.info('did setup translations');

                                // get together a list of natural languages that could be added because they are not
                                // already in the list shown.

                                $scope.addableNaturalLanguages = _.filter(
                                    naturalLanguageData,
                                    function (d) {
                                        return d.code != constants.NATURALLANGUAGECODE_ENGLISH && !_.find(
                                            $scope.translations,
                                            function (t) {
                                                return d.code == t.naturalLanguage.code;
                                            }
                                        );
                                    }
                                );

                                if (0 != $scope.addableNaturalLanguages.length) {
                                    $scope.selectedAddableNaturalLanguage = $scope.addableNaturalLanguages[0];
                                }

                            },
                            function(jsonRpcEnvelope) {
                                errorHandling.handleJsonRpcError(jsonRpcEnvelope);
                            }
                        );
                    },
                    function() {
                        errorHandling.navigateToError();
                    }
                );
            }

            // pulls the pkg data back from the server so that it can be used to
            // display the form.

            function refetchPkg() {
                pkg.getPkgWithSpecificVersionFromRouteParams($routeParams, false).then(
                    function(result) {
                        $scope.pkg = result;
                        $log.info('found '+result.name+' pkg');
                        refreshBreadcrumbItems();
                    },
                    function(err) {
                        errorHandling.handleJsonRpcError(err);
                    }
                );
            }

            // --------------------------
            // ADD A NATURAL LANGUAGE

            /**
             * <p>Add a new translation into the list based on a natural language.  This will also remove the
             * added language from the addable languages list.  It will then choose a new selected language.
             * </p>
             */

            $scope.goAddSelectedAddableNaturalLanguage = function() {

                var translation = {
                    naturalLanguage: $scope.selectedAddableNaturalLanguage,
                    summary: '',
                    description: '',
                    wasEdited: false
                };

                $scope.translations.push(translation);
                $scope.originalTranslations.push(_.clone(translation));

                $scope.addableNaturalLanguages = _.without(
                    $scope.addableNaturalLanguages,
                    $scope.selectedAddableNaturalLanguage);

                $scope.selectedAddableNaturalLanguage = $scope.addableNaturalLanguages.length ? $scope.addableNaturalLanguages[0] : undefined;

                $scope.goChooseTranslation(translation);
            };

            // --------------------------
            // SAVE CHANGES

            /**
             * <p>It is possible to save the translations if something has been edited and if there are no validity
             * problems with the translations.</p>
             */

            $scope.canSave = function() {
                return !!_.findWhere(
                    $scope.translations,
                    { wasEdited : true }
                ) &&
                    !_.find(
                        $scope.translations,
                        function(t) {
                            return !$scope.isTranslationValid(t);
                        }
                    );
            };

            /**
             * <p>This method will persist those changes to the translations back into the server.</p>
             */

            $scope.saveEditedLocalizations = function() {

                if(!$scope.canSave()) {
                    throw Error('not possible to save edited localizations');
                }

                $scope.amSaving = true;

                jsonRpc.call(
                    constants.ENDPOINT_API_V1_PKG,
                    'updatePkgVersionLocalization',
                    [{
                        pkgName: $routeParams.name,
                        architectureCode: $routeParams.architectureCode,
                        replicateToOtherArchitecturesWithSameEnglishContent: $scope.selectedArchitectureApplicability == ARCHITECTUREAPPLICABILITY_ALL,
                        pkgVersionLocalizations: _.map(
                            _.filter(
                                $scope.translations,
                                function(t) { return t.wasEdited; }
                            ),
                            function(t) {
                                return {
                                    naturalLanguageCode : t.naturalLanguage.code,
                                    summary : t.summary,
                                    description : t.description
                                };
                            }
                        )
                    }]
                ).then(
                    function() {
                        $log.info('updated localization on '+$routeParams.name+' pkg');
                        breadcrumbs.popAndNavigate();
                    },
                    function(err) {
                        errorHandling.handleJsonRpcError(err);
                    }
                );

            };

            // --------------------------
            // INIT

            refetchPkg();
            setupTranslations();

            // --------------------------
            // EVENT HANDLING

            // this watch will keep an eye on the summary and description.  If they have changed in isolation (ie;
            // the selected translation has not changed, then we can mark the translation as edited.

            $scope.$watch(
                'selectedTranslation',
                function(newValue, oldValue) {
                    if(null!=oldValue) {
                        if(oldValue.naturalLanguage.code == newValue.naturalLanguage.code &&
                            (oldValue.summary != newValue.summary || oldValue.description != newValue.description) ) {

                            // quick check to see if the new values equal the original values.

                            var originalTranslation = findOriginalTranslation(newValue.naturalLanguage.code);
                            $scope.selectedTranslation.wasEdited = (originalTranslation.summary != newValue.summary) ||
                                (originalTranslation.description != newValue.description);
                        }
                    }
                },
                true);

        }
    ]
);