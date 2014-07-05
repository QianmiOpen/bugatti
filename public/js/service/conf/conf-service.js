'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.service.conf.confModule', []);

    app.factory('ConfService', function($http) {
        return {
            get: function(id, callback) {
                $http(PlayRoutes.controllers.conf.ConfController.show(id)).success(callback);
            },
            getAll: function(eid, vid, callback) {
                $http(PlayRoutes.controllers.conf.ConfController.all(eid, vid)).success(callback);
            },
            save: function(conf, callback) {
                $http.post(PlayRoutes.controllers.conf.ConfController.save().url, conf).success(callback)
            },
            update: function(id, conf, callback) {
                $http.put(PlayRoutes.controllers.conf.ConfController.update(id).url, conf).success(callback)
            },
            remove: function(id, callback) {
                $http(PlayRoutes.controllers.conf.ConfController.delete(id)).success(callback);
            }
        }
    });

    // logs
    app.factory('LogService', function($http) {
        return {
            get: function(id, callback) {
                $http(PlayRoutes.controllers.conf.ConfController.log(id)).success(callback);
            },
            getPage: function(cid, page, pageSize, callback) {
                $http(PlayRoutes.controllers.conf.ConfController.logs(cid, page, pageSize)).success(callback);
            },
            count: function(cid, callback) {
                $http(PlayRoutes.controllers.conf.ConfController.logsCount(cid)).success(callback);
            }
        }
    });

});