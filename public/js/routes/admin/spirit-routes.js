'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.route.admin.spiritModule', []);

    app.config(['$stateProvider', function($stateProvider) {

        // 网关
        $stateProvider.state('admin.spirit', {
            url: "/spirit",
            templateUrl: "partials/admin/spirit/spirit-index.html",
            controller: "SpiritCtrl"
        });

        $stateProvider.state('admin.spirit.create', {
            url: "/create",
            views: {
                "@admin": {
                    templateUrl: "partials/admin/spirit/spirit-new.html",
                    controller: "SpiritCreateCtrl"
                }
            }
        });

        $stateProvider.state('admin.spirit.edit', {
            url: "/:id/edit",
            views: {
                "@admin": {
                    templateUrl: "partials/admin/spirit/spirit-new.html",
                    controller: "SpiritUpdateCtrl"
                }
            }
        });
    }]);

});