'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.route.admin.scriptModule', []);

    app.config(['$stateProvider', function ($stateProvider) {

        $stateProvider.state("admin.script", {
            url: "/script",
            templateUrl: "partials/admin/script/script-index.html",
            controller: "ScriptCtrl"
        });
    }]);

});