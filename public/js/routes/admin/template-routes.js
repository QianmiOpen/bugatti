'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.route.admin.templateModule', []);

    app.config(['$stateProvider', function($stateProvider) {

        // 项目
        $stateProvider.state('admin.template', {
            url: "/template",
            templateUrl: "partials/admin/template/template-index.html",
            controller: 'TemplateCtrl'
        });

        $stateProvider.state('admin.template.create', {
            url: "/create",
            views: {
                "@admin": {
                    templateUrl: "partials/admin/template/template-new.html",
                    controller: 'TemplateCreateCtrl'
                }
            }
        });

        $stateProvider.state('admin.template.edit', {
            url: "/:id/edit",
            views: {
                "@admin": {
                    templateUrl: "partials/admin/template/template-edit.html",
                    controller: 'TemplateUpdateCtrl'
                }
            }
        });

        $stateProvider.state('admin.template.detail', {
            url: "/:id",
            views: {
                "@admin": {
                    templateUrl:"partials/admin/template/template-show.html",
                    controller: 'TemplateShowCtrl'
                }
            }
        });

    }]);

});