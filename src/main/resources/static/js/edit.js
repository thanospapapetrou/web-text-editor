const SAVE_PERIOD = 10000;

function load() {
    document.getElementById('content').disabled = true;
    const name = parseQueryParameters().get('file');
    const request = new XMLHttpRequest();
    request.onload = function () {
        if (this.status == 404) {
            alert('File ' + name + ' not found');
        } else {
            renderFile(JSON.parse(this.response));
        }
    };
    request.open('GET', '/files/' + name, true);
    request.send();
    setTimeout(save, SAVE_PERIOD);
}

function save() {
    document.getElementById('content').disabled = true;
    const name = parseQueryParameters().get('file');
    const request = new XMLHttpRequest();
    request.onload = function () {
        renderFile(JSON.parse(this.response).file);
    };
    request.onerror = function () {
        if (this.status == 409) {
            renderFile(JSON.parse(this.response).file);
        }
    };
    request.open('PUT', '/files/' + name, true);
    request.setRequestHeader('Content-Type', 'application/json');
    request.send(JSON.stringify({lastUpdated: parseInt(document.getElementById('lastUpdated').value), content: document.getElementById('content').value}));
    setTimeout(save, SAVE_PERIOD);
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

function renderFile(file) {
    document.getElementById('name').textContent = file.name;
    document.getElementById('lastUpdated').value = file.lastUpdated;
    document.getElementById('lastUpdatedString').textContent = new Date(file.lastUpdated);
    document.getElementById('content').value = file.content;
    document.getElementById('content').disabled = false;
    document.getElementById('content').focus();
}
