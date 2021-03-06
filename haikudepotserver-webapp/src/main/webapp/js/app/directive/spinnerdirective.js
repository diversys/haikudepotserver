/*
 * Copyright 2013, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

/**
 * <p>This directive will render a DIV that covers the whole page with a spinner inside it.  This provides two
 * functions; the first is to indicate to the user that something is going on and they ought to wait a moment.
 * The second function is to cover the UI so that the user is not able to interact with the page while the
 * operation is being undertaken.</p>
 */

angular.module('haikudepotserver').directive('spinner',function() {
    return {
        restrict: 'E',
        templateUrl:'/js/app/directive/spinner.html',
        replace: true,
        scope: {
            spin: '='
        },
        controller:
            ['constants','$scope','$timeout',
                function(constants,$scope,$timeout) {

                    var timeoutHandle = undefined;

                    $scope.delayedSpin = false;

                    $scope.$watch('spin', function(newSpin) {
                        if(timeoutHandle) {
                            $timeout.cancel(timeoutHandle);
                            timeoutHandle = undefined;
                        }

                        if(!newSpin) {
                            $scope.delayedSpin = false;
                        }
                        else {
                            timeoutHandle = $timeout(function() {
                               $scope.delayedSpin = true;
                            },
                            constants.DELAY_SPINNER);
                        }
                    })

                }
            ]
    };
});