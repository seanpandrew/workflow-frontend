import angular from 'angular';

import 'lib/date-service';
import 'lib/filters-service';
import 'lib/feature-switches';
import 'lib/prodoffice-service';
import 'components/location-picker/location-picker';
import 'components/sidebar-filter/sidebar-filter';
import { filterDefaults } from 'lib/filter-defaults';

import './dashboard-sidebar.html';

angular.module('wfDashboardSidebar', ['wfFiltersService', 'wfSidebarFilter', 'wfProdOfficeService', 'wfLocationPicker'])
    .controller('wfDashboardSidebarController', ['$scope', 'statuses', 'wfFiltersService', 'wfFeatureSwitches', 'wfDateParser', 'wfProdOfficeService', 'sections', function ($scope, statuses, wfFiltersService, wfFeatureSwitches, prodOfficeService, sections) {

        $scope.statuses = statuses;

        $scope.filters = filterDefaults(statuses, wfFiltersService, wfFeatureSwitches);

        function enableSidebar() {
            $scope.enabled = "active";
        }

        function disableSidebar() {
            $scope.enabled = "inactive";
        }

        $scope.$on("search-mode.enter", disableSidebar);
        $scope.$on("search-mode.exit",  enableSidebar);

        // default to enabled
        enableSidebar();
    }]);
