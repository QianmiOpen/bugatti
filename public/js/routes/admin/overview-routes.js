'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.route.admin.overviewModule', []);

    app.config(['$stateProvider', function ($stateProvider) {

        $stateProvider.state("admin.overview", {
            url: "/overview",
            templateUrl: "partials/admin/overview-index.html",
            controller: "OverviewCtrl"
        });

    }]);
});