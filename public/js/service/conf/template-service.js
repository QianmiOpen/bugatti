'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.service.conf.templateModule', []);

    app.factory('TemplateService', function($http) {
        return {
            get: function(id, callback) {
                $http(PlayRoutes.controllers.conf.TemplateController.show(id)).success(callback);
            },
            all: function(callback) {
                $http(PlayRoutes.controllers.conf.TemplateController.all()).success(callback);
            },
            save: function(template, callback) {
                $http.post(PlayRoutes.controllers.conf.TemplateController.save().url, template).success(callback)
            },
            remove: function(id, callback) {
                $http(PlayRoutes.controllers.conf.TemplateController.delete(id)).success(callback);
            },
            items: function(tid, callback) {
                $http(PlayRoutes.controllers.conf.TemplateController.items(tid)).success(callback);
            },
            update: function(id, template, callback) {
                $http.put(PlayRoutes.controllers.conf.TemplateController.update(id).url, template).success(callback)
            }
        }
    });

});