'use strict';

define(['angular'], function(angular) {

    var app = angular.module('bugattiApp.route.profile.profileModule', []);

    app.config(['$stateProvider', function($stateProvider) {

        $stateProvider.state('profile.index',{
            url: "/index"
        });

        $stateProvider.state('profile.keys',{
            url: "/keys"
        });

        $stateProvider.state('profile.env',{
            url: "/env"
        });

        $stateProvider.state('profile.project',{
            url: "/project"
        });


    }]);

});