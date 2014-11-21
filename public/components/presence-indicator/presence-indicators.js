function wfPresenceIndicatorsDirective ($rootScope, wfPresenceService, $log) {

    return {
        restrict: 'E',
        templateUrl: "/assets/components/presence-indicator/presence-indicators.html",
        scope: {
            id: "=presenceId",
            inDrawer: "=inDrawer"
        },
        link: ($scope) => {

            function applyCurrentState(currentState) {
                if(currentState.length === 0) {
                    $scope.presences = [{ status: "free", indicatorText: ""}];
                } else {
                    $scope.presences = _.map(
                        _.uniq(currentState, false, (s) => { return s.clientId.person.email; }),
                        (pr) => {
                            var person = pr.clientId.person;
                            return { indicatorText:
                                (person.firstName.charAt(0) + person.lastName.charAt(0)).toUpperCase(),
                                longText: [person.firstName, person.lastName].join(" "),
                                email: person.email,
                                status: "present" };
                        });
                }
            }

            $scope.$watch("id", (newValue, oldValue) => {
                // may be undefined
                if(!newValue) return;
                console.log("PMR - attempting to get initialData");
                wfPresenceService.initialData(newValue).then((currentState) => {
                    console.log("PMR - got initialData");
                    applyCurrentState(currentState);
                }, (err) => {
                    $log.error("Error getting initial data:", err);
                });

                $scope.$on("presence.status", (ev, data) => {
                    if(newValue === data.subscriptionId) {
                        applyCurrentState(data.currentState);
                    }
                });
            });
        }
    };
}

export { wfPresenceIndicatorsDirective };
