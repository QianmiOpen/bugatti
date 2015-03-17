'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.route.admin.envModule', []);

    app.config(['$stateProvider', function($stateProvider) {

        // 环境
        $stateProvider.state('admin.env', {
            url: "/env",
            templateUrl: "partials/admin/env/env-index.html",
            controller: "EnvCtrl"
        });

        $stateProvider.state('admin.env.create', {
            url: "/create",
            views: {
                "@admin": {
                    templateUrl: "partials/admin/env/env-new.html",
                    controller: "EnvCreateCtrl"
                }
            }
        });

        $stateProvider.state('admin.env.edit', {
            url: "/:id/edit",
            views: {
                "@admin": {
                    templateUrl: "partials/admin/env/env-new.html",
                    controller: "EnvUpdateCtrl"
                }
            }
        });

        $stateProvider.state('admin.env.detail', {
            url: "/:id",
            views: {
                "@admin": {
                    templateUrl:"partials/admin/env/env-show.html",
                    controller: "EnvShowCtrl"
                }
            }
        });


    }]);

});