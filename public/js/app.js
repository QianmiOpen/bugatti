/*global define, angular */

'use strict';

requirejs.config({
    paths: {
        "ace": webjars.path('ace', 'src-min-noconflict/ace'),
        "angular-ui-ace" : webjars.path('angular-ui-ace', 'ui-ace')
    },
    shim: { "angular-ui-ace": [ "angular", "ace"] }
});

require(['angular', './controller/main-controller', './directive/main-directive', './filter/main-filter', './service/main-service', './routes/main-routes', 'angular-ui-router', 'angular-animate', 'ui-bootstrap-tpls', 'angular-sanitize', 'angular-cookies', 'angular-ui-ace'],
    function(angular) {

        // Declare app level module which depends on filters, and services
        angular.module('bugattiApp', [
            'ui.router',
            'ui.ace',
            'ui.bootstrap',
            'ngSanitize',
            'ngAnimate',
            'ngCookies',
            'bugattiApp.routes',
            'bugattiApp.filters',
            'bugattiApp.directives',
            'bugattiApp.services',
            'bugattiApp.controllers'
            ]).run(['$rootScope', '$state', '$stateParams', 'Auth', function($rootScope,   $state,   $stateParams, Auth) {
                $rootScope.$state = $state;
                $rootScope.$stateParams = $stateParams;

                $rootScope.$on("$stateChangeStart", function (event, toState) {
                    Auth.ping(function() {
                        console.log('state.data.access=' + toState.data.access);
                        if (toState.data.access === 'anon') {
                            return
                        }
                        if (!Auth.authorize(toState.data.access)) {
                            event.preventDefault();
                            $state.go('home');
                        }
                    }, function(r) {
                        event.preventDefault();
                        $state.go('home');
                        $rootScope.error = "Unauthorized";
                    });
                });

            }]);

        angular.bootstrap(document, ['bugattiApp']);

    });