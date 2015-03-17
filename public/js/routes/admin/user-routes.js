'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.route.admin.userModule', []);

    app.config(['$stateProvider', function($stateProvider) {

        // 用户
        $stateProvider.state('admin.user', {
            url: "/user",
            templateUrl: "partials/admin/user/user-index.html",
            controller: "UserCtrl"
        });

        $stateProvider.state('admin.user.create', {
            url: "/create",
            views: {
                "@admin": {
                    templateUrl: "partials/admin/user/user-new.html",
                    controller: "UserCreateCtrl"
                }
            }
        });

        $stateProvider.state('admin.user.edit', {
            url: "/:id/edit",
            views: {
                "@admin": {
                    templateUrl: "partials/admin/user/user-edit.html",
                    controller: "UserUpdateCtrl"
                }
            }
        });

        $stateProvider.state('admin.user.detail', {
            url: "/:id",
            views: {
                "@admin": {
                    templateUrl:"partials/admin/user/user-show.html",
                    controller: "UserShowCtrl"
                }
            }
        });

    }]);

});