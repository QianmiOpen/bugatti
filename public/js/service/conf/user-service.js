'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.service.conf.userModule', []);

    app.factory('UserService', function($http) {
        return {
            get: function(jobNo, callback) {
                $http(PlayRoutes.controllers.conf.UserController.show(jobNo)).success(callback);
            },
            getPage: function(jobNo, page, pageSize, callback) {
                $http(PlayRoutes.controllers.conf.UserController.index(jobNo, page, pageSize)).success(callback);
            },
            count: function(jobNo, callback) {
                $http(PlayRoutes.controllers.conf.UserController.count(jobNo)).success(callback);
            },
            save: function(user, callback) {
                $http.post(PlayRoutes.controllers.conf.UserController.save().url, user).success(callback)
            },
            update: function(jobNo, user, callback) {
                $http.put(PlayRoutes.controllers.conf.UserController.update(jobNo).url, user).success(callback)
            },
            remove: function(jobNo, callback) {
                $http(PlayRoutes.controllers.conf.UserController.delete(jobNo)).success(callback);
            }
        }
    });


});