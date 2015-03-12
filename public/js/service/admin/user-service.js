'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.service.admin.userModule', []);

    app.factory('UserService', function($http) {
        return {
            get: function(jobNo, callback) {
                $http(PlayRoutes.controllers.admin.UserController.show(jobNo)).success(callback);
            },
            getPage: function(jobNo, page, pageSize, callback) {
                $http(PlayRoutes.controllers.admin.UserController.index(jobNo, page, pageSize)).success(callback);
            },
            count: function(jobNo, callback) {
                $http(PlayRoutes.controllers.admin.UserController.count(jobNo)).success(callback);
            },
            save: function(user, callback) {
                $http.post(PlayRoutes.controllers.admin.UserController.save().url, user).success(callback)
            },
            update: function(jobNo, user, callback) {
                $http.put(PlayRoutes.controllers.admin.UserController.update(jobNo).url, user).success(callback)
            },
            remove: function(jobNo, callback) {
                $http(PlayRoutes.controllers.admin.UserController.delete(jobNo)).success(callback);
            }
        }
    });


});