/*global define */

'use strict';

define(['angular',
    './admin/user-service',
    './admin/area-service',
    './admin/env-service',
    './admin/spirit-service',
    './admin/project-service',
    './admin/template-service',
    './admin/conf-service',
    './admin/relation-service',
    './admin/script-service',
    './admin/logs-service',
    './admin/system-service',
    './home/task-service'
], function(angular) {

    /* Services */

// Demonstrate how to register services
// In this case it is a simple value service.
    var app = angular.module('bugattiApp.services', [
        'bugattiApp.service.admin.userModule',
        'bugattiApp.service.admin.areaModule',
        'bugattiApp.service.admin.envModule',
        'bugattiApp.service.admin.spiritModule',
        'bugattiApp.service.admin.projectModule',
        'bugattiApp.service.admin.templateModule',
        'bugattiApp.service.admin.confModule',
        'bugattiApp.service.admin.relationModule',
        'bugattiApp.service.admin.scriptModule',
        'bugattiApp.service.admin.logsModule',
        'bugattiApp.service.admin.systemModule',
        'bugattiApp.service.home.taskModule'
    ]);

    app.value('version', '0.1');


    // Auth
    app.factory('Auth', ['$http', '$cookieStore', '$cookies', function ($http, $cookieStore, $cookies) {
        var currentUser = {username: '', role: ''};

        function changeUser(user) {
            angular.extend(currentUser, user)
        }

        return {
            authorize: function(access) {
                if (currentUser.role === 'admin') return true;
                else if (access === 'admin' && access != currentUser.role) return false;
                return true;
            },
            ping: function(success, error) {
                $http.get('/ping').success(function(r) {
                    changeUser({username: r.jobNo, role: r.role});
                    success();
                }).error(function(r) {
                    changeUser({username: '', role: ''});
                    error();
                });
            },
            login: function(user, success, error) {
                $http.post('/login', user).success(function(r) {
                    changeUser({username: r.jobNo, role: r.role});
                    success();
                }).error(error);
            },
            logout: function(success, error) {
                $http.post('/logout').success(function() {
                    changeUser({username: '', role: ''});
                    success();
                }).error(error);
            },
            user: currentUser
        };


    }]);

});