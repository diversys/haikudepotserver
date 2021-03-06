/*
 * Copyright 2014, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

angular.module('haikudepotserver').controller(
    'AboutController',
    [
        '$scope','$log','$location',
        'jsonRpc','constants','userState',
        'breadcrumbs','breadcrumbFactory','errorHandling',
        function(
            $scope,$log,$location,
            jsonRpc,constants,userState,
            breadcrumbs,breadcrumbFactory,errorHandling) {

            breadcrumbs.mergeCompleteStack([
                breadcrumbFactory.createHome(),
                breadcrumbFactory.applyCurrentLocation(breadcrumbFactory.createAbout())
            ]);

            $scope.serverStartTimestamp = undefined;
            $scope.serverProjectVersion = '...';

            // -------------------
            // USER

            function refreshAuthorization() {

                function disallowAll() {
                    $scope.canGoRuntimeInformation = false;
                }

                var u = userState.user();

                if(!u || !u.nickname) {
                    disallowAll();
                }
                else {
                    jsonRpc.call(
                            constants.ENDPOINT_API_V1_USER,
                            'getUser',
                            [{
                                nickname : u.nickname
                            }]
                        ).then(
                        function(result) {
                            $scope.canGoRuntimeInformation = !!result.isRoot;
                        },
                        function(err) {
                            errorHandling.handleJsonRpcError(err);
                        }
                    );
                }
            }

            refreshAuthorization();

            // -------------------
            // RUNTIME INFORMATION

            function refreshRuntimeInformation() {
                jsonRpc.call(
                        constants.ENDPOINT_API_V1_MISCELLANEOUS,
                        "getRuntimeInformation",
                        [{}]
                    ).then(
                    function(result) {
                        $scope.serverProjectVersion = result.projectVersion;
                        $log.info('have fetched the runtime information');
                    },
                    function(err) {
                        errorHandling.handleJsonRpcError(err);
                    }
                );
            }

            refreshRuntimeInformation();

            $scope.canGoRuntimeInformation = false;

            $scope.goRuntimeInformation = function() {
                breadcrumbs.pushAndNavigate(breadcrumbFactory.createRuntimeInformation());
            }

        }
    ]
);