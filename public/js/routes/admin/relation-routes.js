'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.route.admin.relationModule', []);

    app.config(['$stateProvider', function($stateProvider) {

        // 关系（环境和项目）
        $stateProvider.state('admin.relation', {
            url: "/relation",
            templateUrl: "partials/admin/relation/relation-index.html",
            controller: 'RelationCtrl',
            data: { access: 'relation' }
        });


        $stateProvider.state('admin.relation.create', {
            url: "/create",
            views: {
                "@admin": {
                    templateUrl: "partials/admin/relation/relation-new.html",
                    controller: "RelationCreateCtrl"
                }
            }
        });

        $stateProvider.state('admin.relation.detail', {
            url: "/:id",
            views: {
                "@admin": {
                    templateUrl: "partials/admin/relation/relation-show.html",
                    controller: "RelationShowCtrl"
                }
            }
        });


    }]);

});