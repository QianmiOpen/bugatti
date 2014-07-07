'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.route.conf.relationModule', []);

    app.config(['$stateProvider', function($stateProvider) {

        // 关系（环境和项目）
        $stateProvider.state('conf.relation', {
            url: "/relation",
            templateUrl: "partials/conf/relation/relation-index.html",
            controller: 'RelationCtrl'
        });


        $stateProvider.state('conf.relation.create', {
            url: "/create",
            views: {
                "@conf": {
                    templateUrl: "partials/conf/relation/relation-new.html",
                    controller: "RelationCreateCtrl"
                }
            }
        });

    }]);

});