'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.route.admin.projectModule', []);

    app.config(['$stateProvider', function($stateProvider) {

        // 项目
        $stateProvider.state('admin.project', {
            url: "/project",
            templateUrl: "partials/admin/project/project-index.html",
            controller: "ProjectCtrl"
        });

        $stateProvider.state('admin.project.create', {
            url: "/create",
            views: {
                "@admin": {
                    templateUrl: "partials/admin/project/project-new.html",
                    controller: "ProjectCreateCtrl"
                }
            }
        });

        $stateProvider.state('admin.project.edit', {
            url: "/:id/edit",
            views: {
                "@admin": {
                    templateUrl: "partials/admin/project/project-edit.html",
                    controller: "ProjectUpdateCtrl"
                }
            }
        });

        $stateProvider.state('admin.project.detail', {
            url: "/:id",
            views: {
                "@admin": {
                    templateUrl:"partials/admin/project/project-show.html",
                    controller: "ProjectShowCtrl"
                }
            }
        });

        // ===================================================================
        // 子项目,项目下版本
        // ===================================================================
        $stateProvider.state('admin.project.version', {
            url: "/:id/version",
            views: {
                "@admin": {
                    templateUrl: "partials/admin/project/version-index.html",
                    controller: 'VersionCtrl'
                }
            }
        });

        $stateProvider.state('admin.project.version.create', {
            url: "/create",
            views: {
                "@admin": {
                    templateUrl: "partials/admin/project/version-new.html",
                    controller: "VersionCreateCtrl"
                }
            }
        });

        $stateProvider.state('admin.project.version.edit', {
            url: "/:vid/edit",
            views: {
                "@admin": {
                    templateUrl: "partials/admin/project/version-edit.html",
                    controller: "VersionUpdateCtrl"
                }
            }
        });

        /**
         * 项目依赖
         */
        $stateProvider.state('admin.project.dependency', {
            url: "/:id/dependency",
            views: {
                "@admin": {
                    templateUrl: "partials/home/project-dependency.html",
                    controller: "DependencyCtrl"
                }
            }
        })
    }]);

});