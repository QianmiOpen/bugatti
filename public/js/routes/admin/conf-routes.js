'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.route.admin.confModule', []);

    app.config(['$stateProvider', function($stateProvider) {

        // 项目版本下的配置
        $stateProvider.state('admin.project.version.conf', {
            url: "/:vid/conf?eid",
            views: {
                "@admin": {
                    templateUrl: "partials/admin/project/conf-index.html",
                    controller: "ConfCtrl"
                }
            }
        });

        $stateProvider.state('admin.project.version.conf.list', {
            url: "/list",
            views: {
                "conf-info@admin.project.version.conf": {
                    templateUrl: "partials/admin/project/uiview/conf-list.html",
                    controller: 'ConfListCtrl'
                }
            }
        });

        $stateProvider.state('admin.project.version.conf.create', {
            url: "/create",
            views: {
                "conf-info@admin.project.version.conf": {
                    templateUrl: "partials/admin/project/uiview/conf-new.html",
                    controller: "ConfCreateCtrl"
                }
            }
        });

        $stateProvider.state('admin.project.version.conf.upload', {
            url: "/upload",
            views: {
                "conf-info@admin.project.version.conf": {
                    templateUrl: "partials/admin/project/uiview/conf-upload.html",
                    controller: 'ConfUploadCtrl'
                }
            }
        });

        $stateProvider.state('admin.project.version.conf.copy', {
            url: "/copy",
            views: {
                "conf-info@admin.project.version.conf": {
                    templateUrl: "partials/admin/project/uiview/conf-copy.html",
                    controller: 'ConfCopyCtrl'
                }
            }
        });

        $stateProvider.state('admin.project.version.conf.detail', {
            url: "/:cid",
            views: {
                "conf-info@admin.project.version.conf": {
                    templateUrl: "partials/admin/project/uiview/conf-show.html",
                    controller: "ConfShowCtrl"
                }
            }
        });

        $stateProvider.state('admin.project.version.conf.edit', {
            url: "/:cid/edit",
            views: {
                "conf-info@admin.project.version.conf": {
                    templateUrl: "partials/admin/project/uiview/conf-edit.html",
                    controller: "ConfEditCtrl"
                }
            }
        });

        $stateProvider.state('admin.project.version.conf.log', {
            url: "/:cid/log",
            views: {
                "conf-info@admin.project.version.conf": {
                    templateUrl: "partials/admin/project/uiview/log-index.html",
                    controller: "ConfLogCtrl"
                }
            }
        });

        $stateProvider.state('admin.project.version.conf.log.detail', {
            url: "/:lid",
            views: {
                "conf-info@admin.project.version.conf": {
                    templateUrl: "partials/admin/project/uiview/log-show.html",
                    controller: "ConfLogShowCtrl"
                }
            }
        });


    }]);

});