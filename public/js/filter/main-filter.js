/*global define */

'use strict';

define(['angular'], function(angular) {

    /* Filters */

    var app = angular.module('bugattiApp.filters', []);

    // 角色转换显示
    app.filter('role', function() {
        return function(input) {
            return input === 'admin' ? '\u7ba1\u7406\u5458' : '\u7528\u6237'
        };
    });

    // 锁定转换显示
    app.filter('lock', function() {
        return function(input) {
            return input ? '\u9501\u5b9a' : '\u6b63\u5e38'
        };
    });

    // 级别转换显示
    app.filter('level', function() {
        return function(input) {
            return input === 'safe' ? '\u5b89\u5168' : '\u666e\u901a'
        };
    });

    // 数字区间, use: {{ [] | range:1:10 }}
    app.filter('range', function() {
        return function(input, min, max) {
            min = parseInt(min), max = parseInt(max);
            for (var i = min; i <= max; i++)
                input.push(i);
            return input;
        };
    });

});