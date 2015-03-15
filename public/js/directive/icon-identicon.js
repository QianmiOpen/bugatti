/*jslint indent:2*/

// UMD pattern: https://github.com/umdjs/umd/blob/master/returnExports.js
(function (root, factory) {
    'use strict';
    if (typeof define === 'function' && define.amd) {
        // AMD. Register as an anonymous module.
        define(['angular', 'identicon-canvas'], factory);
    } else if (typeof exports === 'object') {
        // Node. Does not work with strict CommonJS, but
        // only CommonJS-like environments that support module.exports,
        // like Node.
        module.exports = factory(require('angular'), require('../thirdparty/identicon-canvas'));
    } else {
        // Browser globals (root is window)
        factory(root.angular, root.identiconCanvas);
    }
}(this, function (angular, identiconCanvas) {
    'use strict';

    var module;

    module = angular.module('icon-identicon', []);

    module.directive('iconIdenticon', [
        '$window',
        function ($window) {
            return {
                restrict: 'E',
                replace: false,
                template: '<canvas></canvas>',
                scope: {
                    size: '@',
                    code: '@'
                },
                link: function ($scope, el$, attrs) {
                    var canvas;
                    canvas = el$[0].querySelector('canvas');
                    $scope.$watch('size', function (result) {
                        identiconCanvas.render(canvas, $scope.code, result);
                    });
                    $scope.$watch('code', function (result) {
                        identiconCanvas.render(canvas, identiconCanvas.fixCode(result), $scope.size);
                    });
                }
            };
        }
    ]);

    return module;
}));
