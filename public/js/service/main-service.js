/*global define */

'use strict';

define(['angular',
    './conf/user-service',
    './conf/env-service'
], function(angular) {

    /* Services */

// Demonstrate how to register services
// In this case it is a simple value service.
    var app = angular.module('bugattiApp.services', [
        'bugattiApp.service.conf.userModule',
        'bugattiApp.service.conf.envModule'
    ]);

    app.value('version', '0.1');

});