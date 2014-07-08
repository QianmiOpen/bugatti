'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.route.conf.envModule', []);

    app.config(['$stateProvider', function($stateProvider) {

        // 环境
        $stateProvider.state('conf.env', {
            url: "/env",
            templateUrl: "partials/conf/env/env-index.html",
            controller: "EnvCtrl",
            data: { access: 'env' }
        });

        $stateProvider.state('conf.env.create', {
            url: "/create",
            views: {
                "@conf": {
                    templateUrl: "partials/conf/env/env-new.html",
                    controller: "EnvCreateCtrl"
                }
            }
        });

        $stateProvider.state('conf.env.edit', {
            url: "/:id/edit",
            views: {
                "@conf": {
                    templateUrl: "partials/conf/env/env-new.html",
                    controller: "EnvUpdateCtrl"
                }
            }
        });

        $stateProvider.state('conf.env.detail', {
            url: "/:id",
            views: {
                "@conf": {
                    templateUrl:"partials/conf/env/env-show.html",
                    controller: "EnvShowCtrl"
                }
            }
        });


    }]);

});