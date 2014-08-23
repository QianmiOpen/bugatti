'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.route.conf.relationModule', []);

    app.config(['$stateProvider', function($stateProvider) {

        // 关系（环境和项目）
        $stateProvider.state('conf.relation', {
            url: "/relation",
            templateUrl: "partials/conf/relation/relation-index.html",
            controller: 'RelationCtrl',
            data: { access: 'relation' }
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

        $stateProvider.state('conf.relation.detail', {
            url: "/:id",
            views: {
                "@conf": {
                    templateUrl: "partials/conf/relation/relation-show.html",
                    controller: "RelationShowCtrl"
                }
            }
        });


    }]);

});