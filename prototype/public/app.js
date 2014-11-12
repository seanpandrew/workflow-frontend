/**
 * Main JS module for Workflow's angular app.
 */

import angular from 'angular';

// Legacy:
// import 'javascripts/services';
// import 'javascripts/directives';
// import 'javascripts/controllers';
// import 'javascripts/controllers/dashboard';
// import 'javascripts/controllers/dashboard/content-item';
// import 'javascripts/controllers/dashboard/dashboard';
// import 'javascripts/controllers/dashboard/date-filter';
// import 'javascripts/controllers/dashboard/stub-crud';
// import 'javascripts/services/legal-states-service';
// import 'javascripts/services/prodoffice-service';

import 'components/content-list/content-list';
import 'components/icons/icons';

import 'layouts/dashboard/dashboard';
import 'layouts/dashboard/dashboard-user';
import 'layouts/dashboard/dashboard-toolbar';
import 'layouts/dashboard/dashboard-sidebar';

import 'lib/date-service';
import 'lib/filters-service';
import 'lib/analytics';
import 'lib/feature-switches';

// 3rd party libs
import 'angular-ui-router';
import 'angular-bootstrap';
import 'angular-xeditable';
import 'angular-animate/angular-animate.min';

// App-wide Styles
import 'bootstrap@3.2.0/css/bootstrap.min.css!';
import 'main.min.css!';

angular.module('workflow',
    [
        'ui.router',
        'ngAnimate',
        'wfDashboard',
        'wfDashboardUser',
        'wfDashboardToolbar',
        'wfDashboardSidebar',
        'wfIcons',
        'wfContentList',
        'wfDateService',
        'wfAnalytics',
        'wfFiltersService',
        'wfFeatureSwitches',
        'xeditable'
    ])
    .config(['$stateProvider', '$urlRouterProvider', function ($stateProvider, $urlRouterProvider) {
        // TODO: remember user's state and redirect there on default '' route
        $urlRouterProvider.when('', '/dashboard');

        $stateProvider.state('dashboard', {
            url: '/dashboard',
            views: {
                '': {
                    templateUrl: '/assets/layouts/dashboard/dashboard.html',
                    controller: 'wfDashboardController'
                },
                'view-toolbar': {
                    templateUrl: '/assets/layouts/dashboard/dashboard-toolbar.html',
                    controller: 'wfDashboardToolbarController'
                },
                'view-user': {
                    templateUrl: '/assets/layouts/dashboard/dashboard-user.html',
                    controller: 'wfDashboardUserController'
                }
            }
        });

    }])


    // Global config
    .constant(
        'config',
        {
            'composerNewContent': _wfConfig.composer.create,
            'composerViewContent': _wfConfig.composer.view,
            'composerContentDetails': _wfConfig.composer.details,
            'presenceUrl': _wfConfig.presenceUrl,
            'maxNoteLength': 500
        }
    )
    .constant({ 'statuses': _wfConfig.statuses })
    .constant({ 'sections': _wfConfig.sections })

    // XEditable options, TODO: mode out to dashboard controller somewhere...
    .run(function (editableOptions) {
        editableOptions.theme = 'bs3'; // bootstrap3 theme. Can be also 'bs2', 'default'
    });

// Bootstrap App
angular.element(document).ready(function () {
    angular.bootstrap(document, ['workflow']);
});