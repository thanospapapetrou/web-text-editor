function load() {
    const name = parseQueryParameters().get('file');
    const request = new XMLHttpRequest();
    request.onload = function () {
        const file = JSON.parse(this.response);
        document.getElementById('name').textContent = file.name;
        document.getElementById('content').value = file.content;
        document.getElementById('lastUpdated').value = file.lastUpdated;
        document.getElementById('lastUpdatedString').textContent = new Date(file.lastUpdated);
    }
    request.open('GET', '/files/' + name, true);
    request.send();
}

function save() {
    const name = parseQueryParameters().get('file');
    const request = new XMLHttpRequest();
    request.onload = function () {
        // TODO user may have typed more text before loading, we should only get timestamp back
        load();
    }
    request.open('PUT', '/files/' + name, true);
    request.send(document.getElementById('content').value);
}

function parseQueryParameters() {
    const parameters = new Map();
    window.location.search.split(/[?&]/).forEach(parameter => {
        const nameValue = parameter.split(/=/);
        if (nameValue.length > 1) {
            parameters.set(nameValue[0], nameValue[1]);
        }
    });
    return parameters;
}
