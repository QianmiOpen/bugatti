/*global define */

'use strict';

define(['angular'], function(angular) {

    /* Filters */

    var app = angular.module('bugattiApp.filters', []);

    app.filter('role', function(){
        return function(input) {
            return input === 'admin' ? '\u7ba1\u7406\u5458' : '\u7528\u6237'
        };
    });

    app.filter('lock', function(){
        return function(input) {
            return input ? '\u9501\u5b9a' : '\u6b63\u5e38'
        };
    });

});