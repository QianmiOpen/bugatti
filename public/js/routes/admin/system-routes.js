'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.route.admin.systemModule', []);

    app.config(['$stateProvider', function ($stateProvider) {

        $stateProvider.state("admin.system", {
            url: "/system",
            templateUrl: "partials/admin/system/system-index.html",
            controller: "SystemCtrl"
        });
    }]);
});