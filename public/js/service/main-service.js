/*global define */

'use strict';

define(['angular',
    './conf/user-service',
    './conf/env-service',
    './conf/project-service',
    './conf/template-service',
    './task/task-service'
], function(angular) {

    /* Services */

// Demonstrate how to register services
// In this case it is a simple value service.
    var app = angular.module('bugattiApp.services', [
        'bugattiApp.service.conf.userModule',
        'bugattiApp.service.conf.envModule',
        'bugattiApp.service.conf.projectModule',
        'bugattiApp.service.conf.templateModule',
        'bugattiApp.service.task.taskModule'
    ]);

    app.value('version', '0.1');

});