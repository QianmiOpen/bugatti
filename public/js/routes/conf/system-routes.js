'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.route.conf.systemModule', []);

    app.config(['$stateProvider', function ($stateProvider) {

        $stateProvider.state("conf.system", {
            url: "/system",
            templateUrl: "partials/conf/system/system-index.html",
            controller: "SystemCtrl",
            data: { access: 'system' }
        });
    }]);
});