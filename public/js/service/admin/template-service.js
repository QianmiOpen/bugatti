'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.service.admin.templateModule', []);

    app.factory('TemplateService', function($http) {
        return {
            get: function(id, callback) {
                $http(PlayRoutes.controllers.admin.TemplateController.show(id)).success(callback);
            },
            all: function(callback) {
                $http(PlayRoutes.controllers.admin.TemplateController.all()).success(callback);
            },
            save: function(template, callback) {
                $http.post(PlayRoutes.controllers.admin.TemplateController.save().url, template).success(callback)
            },
            remove: function(id, callback) {
                $http(PlayRoutes.controllers.admin.TemplateController.delete(id)).success(callback);
            },
            update: function(id, template, callback) {
                $http.put(PlayRoutes.controllers.admin.TemplateController.update(id).url, template).success(callback)
            },
            // template items
            items: function(templateId, callback) {
                $http(PlayRoutes.controllers.admin.TemplateController.items(templateId)).success(callback);
            },
            itemAttrs: function(templateId, scriptVersion, callback) {
                $http(PlayRoutes.controllers.admin.TemplateController.itemAttrs(templateId, scriptVersion)).success(callback);
            },
            itemVars: function(templateId, scriptVersion, callback) {
                $http(PlayRoutes.controllers.admin.TemplateController.itemVars(templateId, scriptVersion)).success(callback);
            }
        }
    });

});