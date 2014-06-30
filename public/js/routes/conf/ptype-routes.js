'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.route.conf.ptypeModule', []);

    app.config(['$stateProvider', function($stateProvider) {

        // 项目
        $stateProvider.state('conf.ptype', {
            url: "/ptype",
            templateUrl: "partials/conf/ptype/ptype-index.html"
        });

        $stateProvider.state('conf.ptype.create', {
            url: "/create",
            views: {
                "@conf": {
                    templateUrl: "partials/conf/ptype/ptype-new.html",
                    controller: 'PTypeCreateCtrl'
                }
            }
        });

        $stateProvider.state('conf.ptype.edit', {
            url: "/:id/edit",
            views: {
                "@conf": {
                    templateUrl: "partials/conf/ptype/ptype-new.html"
                }
            }
        });

        $stateProvider.state('conf.ptype.detail', {
            url: "/:id",
            views: {
                "@conf": {
                    templateUrl:"partials/conf/ptype/ptype-show.html"
                }
            }
        });

    }]);

});