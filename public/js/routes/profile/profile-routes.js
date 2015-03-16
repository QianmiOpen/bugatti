'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.route.profile.profileModule', []);

    app.config(['$stateProvider', function($stateProvider) {

        $stateProvider.state('profile.index',{
            url: "/index",
            templateUrl: "partials/profile/profile-index.html",
            controller: 'ProfileCtrl'
        });

        $stateProvider.state('profile.keys',{
            url: "/keys",
            templateUrl: "partials/profile/profile-keys.html",
            controller: 'ProfileKeysCtrl'
        });

        // --------------------------------------------
        // 环境
        // --------------------------------------------
        $stateProvider.state('profile.env',{
            url: "/env",
            templateUrl: "partials/profile/profile-env.html",
            controller: 'ProfileEnvCtrl'
        });

        $stateProvider.state('profile.env.create',{
            url: "/create",
            views: {
                "@profile": {
                    templateUrl: "partials/admin/env/env-new.html",
                    controller: "EnvCreateCtrl"
                }
            }
        });

        $stateProvider.state('profile.env.edit', {
            url: "/:id/edit",
            views: {
                "@profile": {
                    templateUrl: "partials/admin/env/env-new.html",
                    controller: "EnvUpdateCtrl"
                }
            }
        });

        $stateProvider.state('profile.env.detail', {
            url: "/:id",
            views: {
                "@profile": {
                    templateUrl:"partials/admin/env/env-show.html",
                    controller: "EnvShowCtrl"
                }
            }
        });

        // --------------------------------------------
        // 项目
        // --------------------------------------------
        $stateProvider.state('profile.project',{
            url: "/project",
            templateUrl: "partials/profile/profile-project.html",
            controller: 'ProfileProCtrl'
        });


        $stateProvider.state('profile.project.create', {
            url: "/create",
            views: {
                "@profile": {
                    templateUrl: "partials/admin/project/project-new.html",
                    controller: "ProjectCreateCtrl"
                }
            }
        });

        $stateProvider.state('profile.project.edit', {
            url: "/:id/edit",
            views: {
                "@profile": {
                    templateUrl: "partials/admin/project/project-edit.html",
                    controller: "ProjectUpdateCtrl"
                }
            }
        });

        $stateProvider.state('profile.project.detail', {
            url: "/:id",
            views: {
                "@profile": {
                    templateUrl:"partials/admin/project/project-show.html",
                    controller: "ProjectShowCtrl"
                }
            }
        });

        // 项目版本
        $stateProvider.state('profile.project.version', {
            url: "/:id/version",
            views: {
                "@profile": {
                    templateUrl: "partials/admin/project/version-index.html",
                    controller: 'VersionCtrl'
                }
            }
        });

        $stateProvider.state('profile.project.version.create', {
            url: "/create",
            views: {
                "@profile": {
                    templateUrl: "partials/admin/project/version-new.html",
                    controller: "VersionCreateCtrl"
                }
            }
        });

        $stateProvider.state('profile.project.version.edit', {
            url: "/:vid/edit",
            views: {
                "@profile": {
                    templateUrl: "partials/admin/project/version-edit.html",
                    controller: "VersionUpdateCtrl"
                }
            }
        });


    }]);

});