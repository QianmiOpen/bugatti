'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.route.conf.spiritModule', []);

    app.config(['$stateProvider', function($stateProvider) {

        // 网关
        $stateProvider.state('conf.spirit', {
            url: "/spirit",
            templateUrl: "partials/conf/spirit/spirit-index.html",
            controller: "SpiritCtrl",
            data: { access: 'spirit' }
        });

        $stateProvider.state('conf.spirit.create', {
            url: "/create",
            views: {
                "@conf": {
                    templateUrl: "partials/conf/spirit/spirit-new.html",
                    controller: "SpiritCreateCtrl"
                }
            }
        });

        $stateProvider.state('conf.spirit.edit', {
            url: "/:id/edit",
            views: {
                "@conf": {
                    templateUrl: "partials/conf/spirit/spirit-new.html",
                    controller: "SpiritUpdateCtrl"
                }
            }
        });
    }]);

});