'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.route.conf.projectModule', []);

    app.config(['$stateProvider', function($stateProvider) {

        // 项目
        $stateProvider.state('conf.project', {
            url: "/project",
            templateUrl: "partials/conf/project/project-index.html",
            controller: "ProjectCtrl"
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
                    templateUrl: "partials/conf/project/project-new.html"
                }
            }
        });

        $stateProvider.state('conf.project.detail', {
            url: "/:id",
            views: {
                "@conf": {
                    templateUrl:"partials/conf/project/project-show.html"
                }
            }
        });

    }]);

});