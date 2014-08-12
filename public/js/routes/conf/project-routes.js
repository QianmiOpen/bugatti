'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.route.conf.projectModule', []);

    app.config(['$stateProvider', function($stateProvider) {

        // 项目
        $stateProvider.state('conf.project', {
            url: "/project",
            templateUrl: "partials/conf/project/project-index.html",
            controller: "ProjectCtrl",
            data: { access: 'project' }
        });

        $stateProvider.state('conf.project.my', {
            url: "/my",
            views: {
                "@conf": {
                    templateUrl: "partials/conf/project/project-my.html",
                    controller: "ProjectCtrl"
                }
            }
        });

        $stateProvider.state('conf.project.create', {
            url: "/create",
            views: {
                "@conf": {
                    templateUrl: "partials/conf/project/project-new.html",
                    controller: "ProjectCreateCtrl"
                }
            }
        });

        $stateProvider.state('conf.project.edit', {
            url: "/:id/edit",
            views: {
                "@conf": {
                    templateUrl: "partials/conf/project/project-edit.html",
                    controller: "ProjectUpdateCtrl"
                }
            }
        });

        $stateProvider.state('conf.project.detail', {
            url: "/:id",
            views: {
                "@conf": {
                    templateUrl:"partials/conf/project/project-show.html",
                    controller: "ProjectShowCtrl"
                }
            }
        });

        // ===================================================================
        // 子项目,项目下版本
        // ===================================================================
        $stateProvider.state('conf.project.version', {
            url: "/:id/version",
            views: {
                "@conf": {
                    templateUrl: "partials/conf/project/version-index.html",
                    controller: 'VersionCtrl'
                }
            }
        });


        $stateProvider.state('conf.project.version.create', {
            url: "/create",
            views: {
                "@conf": {
                    templateUrl: "partials/conf/project/version-new.html",
                    controller: "VersionCreateCtrl"
                }
            }
        });

        $stateProvider.state('conf.project.version.edit', {
            url: "/:vid/edit",
            views: {
                "@conf": {
                    templateUrl: "partials/conf/project/version-edit.html",
                    controller: "VersionUpdateCtrl"
                }
            }
        });
        /**
         * 项目依赖
         */
        $stateProvider.state('conf.project.dependency', {
            url: "/dependency/:id",
            views: {
                "@conf": {
                    templateUrl: "partials/conf/project/dependency-index.html",
                    controller: "DependencyCtrl"
                }

            }
        })
    }]);

});