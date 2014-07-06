'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.route.conf.confModule', []);

    app.config(['$stateProvider', function($stateProvider) {

        // 项目版本下的配置
        $stateProvider.state('conf.project.version.conf', {
            url: "/:vid/conf?eid",
            views: {
                "@conf": {
                    templateUrl: "partials/conf/project/conf-index.html",
                    controller: "ConfCtrl"
                }
            }
        });

        $stateProvider.state('conf.project.version.conf.list', {
            url: "/list",
            views: {
                "conf-info@conf.project.version.conf": {
                    templateUrl: "partials/conf/project/uiview/conf-list.html",
                    controller: 'ConfListCtrl'
                }
            }
        });

        $stateProvider.state('conf.project.version.conf.create', {
            url: "/create",
            views: {
                "conf-info@conf.project.version.conf": {
                    templateUrl: "partials/conf/project/uiview/conf-new.html",
                    controller: "ConfCreateCtrl"
                }
            }
        });

        $stateProvider.state('conf.project.version.conf.detail', {
            url: "/:cid",
            views: {
                "conf-info@conf.project.version.conf": {
                    templateUrl: "partials/conf/project/uiview/conf-show.html",
                    controller: "ConfShowCtrl"
                }
            }
        });

        $stateProvider.state('conf.project.version.conf.edit', {
            url: "/:cid/edit",
            views: {
                "conf-info@conf.project.version.conf": {
                    templateUrl: "partials/conf/project/uiview/conf-edit.html",
                    controller: "ConfEditCtrl"
                }
            }
        });

        $stateProvider.state('conf.project.version.conf.log', {
            url: "/:cid/log",
            views: {
                "conf-info@conf.project.version.conf": {
                    templateUrl: "partials/conf/project/uiview/log-index.html",
                    controller: "ConfLogCtrl"
                }
            }
        });

        $stateProvider.state('conf.project.version.conf.log.detail', {
            url: "/:lid",
            views: {
                "conf-info@conf.project.version.conf": {
                    templateUrl: "partials/conf/project/uiview/log-show.html",
                    controller: "ConfLogShowCtrl"
                }
            }
        });



    }]);

});