'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.route.conf.templateModule', []);

    app.config(['$stateProvider', function($stateProvider) {

        // 项目
        $stateProvider.state('conf.template', {
            url: "/template",
            templateUrl: "partials/conf/template/template-index.html",
            controller: 'TemplateCtrl'
        });

        $stateProvider.state('conf.template.create', {
            url: "/create",
            views: {
                "@conf": {
                    templateUrl: "partials/conf/template/template-new.html",
                    controller: 'TemplateCreateCtrl'
                }
            }
        });

        $stateProvider.state('conf.template.edit', {
            url: "/:id/edit",
            views: {
                "@conf": {
                    templateUrl: "partials/conf/template/template-edit.html",
                    controller: 'TemplateUpdateCtrl'
                }
            }
        });

        $stateProvider.state('conf.template.detail', {
            url: "/:id",
            views: {
                "@conf": {
                    templateUrl:"partials/conf/template/template-show.html",
                    controller: 'TemplateShowCtrl'
                }
            }
        });

    }]);

});