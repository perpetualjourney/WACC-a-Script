(function() {
    'use strict';
    window.app = angular.module('fileBrowserApp', ['ngRoute', 'jsTree.directive', 'hljs']).
    config(['$routeProvider',
        function($routeProvider) {
            $routeProvider.
            when('/', {
                templateUrl: '../partials/home.html',
                controller: 'HomeCtrl'
            }).
            otherwise({
                redirectTo: '/home'
            });
        }
    ]);
}());