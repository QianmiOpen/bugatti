'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.route.conf.userModule', []);

    app.config(['$stateProvider', function($stateProvider) {

        // 用户
        $stateProvider.state('conf.user', {
            url: "/user",
            templateUrl: "partials/conf/user/user-index.html",
            controller: "UserCtrl"
        });

        $stateProvider.state('conf.user.create', {
            url: "/create",
            views: {
                "@conf": {
                    templateUrl: "partials/conf/user/user-new.html",
                    controller: "UserCreateCtrl"
                }
            }
        });

        $stateProvider.state('conf.user.edit', {
            url: "/:id/edit",
            views: {
                "@conf": {
                    templateUrl: "partials/conf/user/user-edit.html",
                    controller: "UserUpdateCtrl"
                }
            }
        });

        $stateProvider.state('conf.user.detail', {
            url: "/:id",
            views: {
                "@conf": {
                    templateUrl:"partials/conf/user/user-show.html",
                    controller: "UserShowCtrl"
                }
            }
        });

    }]);

});