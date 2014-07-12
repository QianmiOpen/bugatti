/*global define */

'use strict';

define(['angular'], function(angular) {

    /* Directives */

    var app = angular.module('bugattiApp.directives', []);

    /* 返回上一页(浏览器历史) */
    app.directive('backButton', [function() {
        return {
            restrict: 'A',
            link: function(scope, elm, attrs) {
                elm.bind('click', function () {
                    history.back();
                });
            }
        }
    }]);

    /* 用户权限列表展示 */
    app.directive('permission', ['UserService', function(UserService) {
        return {
            restrict: 'E',
            scope: {
                jobNo: '@'
            },
            template: '<ul class="horizonal list-group"><li class="list-group-item" ng-repeat="func in functions">{{func}}</li></ul>',
            link: function($scope, element, attrs) {
                $scope.functions = [];
                UserService.permissions($scope.jobNo, function(data) {
                    angular.forEach(data.functions, function(f) {
                        if (f === 'user') {
                            $scope.functions.push('用户管理');
                        } else if (f === 'area') {
                            $scope.functions.push('区域管理');
                        } else if (f === 'env') {
                            $scope.functions.push('环境管理');
                        } else if (f === 'project') {
                            $scope.functions.push('项目管理');
                        } else if (f === 'relation') {
                            $scope.functions.push('关系配置');
                        } else if (f === 'task') {
                            $scope.functions.push('任务管理');
                        }
                    });
                });
            }
        }
    }]);

    // 模板名称显示
    app.directive('templateShow', ['TemplateService', function(TemplateService) {
        return {
            restrict: 'E',
            scope: {
                tid: '@'
            },
            template: '<span>{{template.name}}</span>',
            link: function($scope, element, attrs) {
                TemplateService.get($scope.tid, function(data) {
                    $scope.template = data;
                });
            }
        }
    }]);

    // 项目名称显示
    app.directive('projectShow', ['ProjectService', function(ProjectService) {
        return {
            restrict: 'E',
            scope: {
                pid: '@'
            },
            template: '<span>{{project.name}}</span>',
            link: function($scope, element, attrs) {
                ProjectService.get($scope.pid, function(data) {
                    $scope.project = data;
                });
            }
        }
    }]);

    // 环境名称显示
    app.directive('envShow', ['EnvService', function(EnvService) {
        return {
            restrict: 'E',
            scope: {
                eid: '@'
            },
            template: '<span>{{env.name}}</span>',
            link: function($scope, element, attrs) {
                EnvService.get($scope.eid, function(data) {
                    $scope.env = data;
                });
            }
        }
    }]);


    // 页面权限
    app.directive('accessPermission', ['Auth', function(Auth) {
        return {
            restrict: 'A',
            link: function($scope, element, attrs) {
                var prevDisp = element.css('display')
                    , access;
                $scope.$watch('user', function(user) {
                    updateCSS();
                }, true);
                attrs.$observe('accessPermission', function(al) {
                    access = al;
                    updateCSS();
                });
                function updateCSS() {
                    if (access) {
                        if (!Auth.authorize(access))
                            element.css('display', 'none');
                        else
                            element.css('display', prevDisp);
                    }
                }
            }
        }
    }]);

    /* 判断用户是否为项目成员 */
    app.directive('hasProject', ['Auth', 'ProjectService', function(Auth, ProjectService) {
        return {
            restrict: 'A',
            scope: false,
            link: function($scope, element, attrs) {
                $scope.hasProject_ = false;

                attrs.$observe('hasProject', function(pid) {
                    updateCSS(pid)
                });
                function updateCSS(pid) {
                    if (Auth.user.role === 'admin') {
                        $scope.hasProject_ = true;
                    }
                    else if (Auth.user.role === 'user') {
                        ProjectService.member(pid, Auth.user.username, function(member) {
                            $scope.hasProject_ = true;
                        })
                    }
                }
            }
        }
    }]);

    /* 判断用户是否为项目管理员 */
    app.directive('hasProjectSafe', ['Auth', 'ProjectService', function(Auth, ProjectService) {
        return {
            restrict: 'A',
            scope: false,
            link: function($scope, element, attrs) {
                $scope.hasProjectSafe_ = false;

                attrs.$observe('hasProjectSafe', function(pid) {
                    updateCSS(pid)
                });
                function updateCSS(pid) {
                    if (Auth.user.role === 'admin') {
                        $scope.hasProjectSafe_ = true;
                    }
                    else if (Auth.user.role === 'user') {
                        ProjectService.member(pid, Auth.user.username, function(member) {
                            if (member != null && member.level == 'safe') {
                                $scope.hasProjectSafe_ = true;
                            }
                        })
                    }
                }
            }
        }
    }]);

    /* diff比较内容 */
    app.directive('diffShow', [function() {
        if (angular.isUndefined(window.difflib)) {
            throw new Error('js diff need js difflib to work...(o rly?)');
        }
        if (angular.isUndefined(window.diffview)) {
            throw new Error('js diff need js diffview to work...(o rly?)');
        }
        return {
            restrict: 'A',
            scope: {
                baseText: '@diffBaseText',
                baseTextName: '@diffBaseTextName',
                newText: '@diffNewText',
                newTextName: '@diffNewTextName',
                viewType : '@diffViewType'
            },
            link: function(scope, iElement, iAttrs) {
                console.log('baseText='+scope.baseText
                +',baseTextName='+scope.baseTextName
                +',newText='+scope.newText
                +',newTextName='+scope.newTextName)

                var base = difflib.stringAsLines(scope.baseText),
                    newtxt = difflib.stringAsLines(scope.newText),
                    sm = new difflib.SequenceMatcher(base, newtxt),
                    opcodes = sm.get_opcodes();
                var diffViewData = diffview.buildView({
                    baseTextLines: base,
                    newTextLines: newtxt,
                    opcodes: opcodes,
                    baseTextName: scope.baseTextName,
                    newTextName: scope.newTextName,
                    viewType: scope.viewType
                });
                iElement.append(diffViewData);
            }
        }
    }]);

});