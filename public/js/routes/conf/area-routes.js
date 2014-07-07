'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.route.conf.areaModule', []);

    app.config(['$stateProvider', function ($stateProvider) {
        $stateProvider.state("conf.area", {
            url: "/area",
            templateUrl: "partials/conf/area/area-index.html",
            controller: "AreaCtrl"
        });

        $stateProvider.state("conf.area.create", {
            url: "/create",
            views: {
                "@conf": {
                    templateUrl: "partials/conf/area/area-new.html",
                    controller: "AreaCreateCtrl"
                }
            }
        });

        $stateProvider.state("conf.area.edit", {
            url: "/:id/edit",
            views: {
                "@conf": {
                    templateUrl: "partials/conf/area/area-new.html",
                    controller: "AreaUpdateCtrl"
                }
            }
        });

    }]);
});