'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.service.conf.templateModule', []);

    app.factory('TemplateService', function($http) {
        return {
            all: function(callback) {
                $http(PlayRoutes.controllers.conf.TemplateController.all()).success(callback);
            }
        }
    });

});