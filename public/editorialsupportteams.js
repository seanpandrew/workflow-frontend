import 'whatwg-fetch';

function addNewStaff(team) {
    var name = document.getElementById("name-entry-"+team).value.replace(/[^\w\s.,!?/]/gi, '');
    var endpoint = `/api/editorialSupportTeams?name=${name}&team=${team}`;
    fetch(endpoint, {
        method: 'PUT',
        credentials: 'same-origin'
    }).then(reloadOnComplete)
}

function reloadOnComplete(response) {
    if (response.status === 200) {
        location.reload();
    }
}

function toggleStaff(id) {
    var active = document.getElementById("editorialSupportStatus-"+id).checked;
    var endpoint = `/api/editorialSupportTeams/toggle?id=${id}&active=${active}`;
    fetch(endpoint, {
        method: 'POST',
        credentials: 'same-origin'
    }).then(reloadOnComplete)
}

function updateStatus(id, startText) {
    var text = document.getElementById("editorialSupportDescription-"+id).value.replace(/[^\w\s.,!?/]/gi, '');
    if (text !== startText) {
        var endpoint = `/api/editorialSupportTeams/update?id=${id}&description=${text}`;
        fetch(endpoint, {
            method: 'POST',
            credentials: 'same-origin'
        }).then(reloadOnComplete)
    }

}

function updateFrontsTeam(id, startText) {
    var text = document.getElementById("frontsTeam-"+id).value.replace(/[^\w\s.,!?/]/gi, '');
    if (text !== startText) {
        var endpoint = `/api/editorialSupportTeams/fronts?id=${id}&activeStaff=${text}`;
        fetch(endpoint, {
            method: 'PUT',
            credentials: 'same-origin'
        }).then(reloadOnComplete)
    }
}

window ? window.addNewStaff = addNewStaff : false
window ? window.toggleStaff = toggleStaff : false
window ? window.updateStatus = updateStatus : false
window ? window.updateFrontsTeam = updateFrontsTeam : false