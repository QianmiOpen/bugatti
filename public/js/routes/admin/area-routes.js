'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.route.admin.areaModule', []);

    app.config(['$stateProvider', function ($stateProvider) {

        $stateProvider.state("admin.area", {
            url: "/area",
            templateUrl: "partials/admin/area/area-index.html",
            controller: "AreaCtrl"
        });

        $stateProvider.state("admin.area.create", {
            url: "/create",
            views: {
                "@admin": {
                    templateUrl: "partials/admin/area/area-new.html",
                    controller: "AreaCreateCtrl"
                }
            }
        });

        $stateProvider.state("admin.area.edit", {
            url: "/:id/edit",
            views: {
                "@admin": {
                    templateUrl: "partials/admin/area/area-new.html",
                    controller: "AreaUpdateCtrl"
                }
            }
        });

    }]);
});