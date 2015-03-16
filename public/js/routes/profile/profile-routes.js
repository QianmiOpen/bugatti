'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.route.profile.profileModule', []);

    app.config(['$stateProvider', function($stateProvider) {

        $stateProvider.state('profile.index',{
            url: "/index",
            templateUrl: "partials/profile/profile-index.html",
            controller: 'ProfileCtrl'
        });

        $stateProvider.state('profile.keys',{
            url: "/keys",
            templateUrl: "partials/profile/profile-keys.html",
            controller: 'ProfileKeysCtrl'
        });

        $stateProvider.state('profile.env',{
            url: "/env",
            templateUrl: "partials/profile/profile-env.html",
            controller: 'ProfileCtrl'
        });

        $stateProvider.state('profile.project',{
            url: "/project",
            templateUrl: "partials/profile/profile-project.html",
            controller: 'ProfileCtrl'
        });


    }]);

});