var modified = false;

function load() {
    document.getElementById('content').disabled = true;
    const name = parseQueryParameters().get('file');
    const request = new XMLHttpRequest();
    request.onload = function () {
        if (this.status == 404) {
            alert('File ' + name + ' not found');
        } else {
            const file = JSON.parse(this.response);
            document.getElementById('name').textContent = file.name;
            document.getElementById('lastUpdated').value = file.lastUpdated;
            document.getElementById('lastUpdatedString').textContent = new Date(file.lastUpdated);
            document.getElementById('content').value = file.content;
            document.getElementById('content').disabled = false;
            document.getElementById('content').focus();
            setTimeout(save, 5000);
        }
    };
    request.open('GET', '/files/' + name, true);
    request.send();
}

function save() {
    if (modified) {
        document.getElementById('content').disabled = true;
        const name = parseQueryParameters().get('file');
        const request = new XMLHttpRequest();
        request.onload = function () {
            const file = JSON.parse(this.response).file;
            document.getElementById('lastUpdated').value = file.lastUpdated;
            document.getElementById('lastUpdatedString').textContent = new Date(file.lastUpdated);
            document.getElementById('content').value = file.content;
            modified = false;
            document.getElementById('content').disabled = false;
            document.getElementById('content').focus();
        };
        request.onerror = function () {
            if (this.status == 409) {
                const file = JSON.parse(this.response).file;
                document.getElementById('lastUpdated').value = file.lastUpdated;
                document.getElementById('lastUpdatedString').textContent = new Date(file.lastUpdated);
                document.getElementById('content').value = file.content;
                modified = false;
                document.getElementById('content').disabled = false;
                document.getElementById('content').focus();
            }
        };
        request.open('PUT', '/files/' + name, true);
        request.setRequestHeader('Content-Type', 'application/json');
        request.send(JSON.stringify({lastUpdated: parseInt(document.getElementById('lastUpdated').value), content: document.getElementById('content').value}));
    }
    setTimeout(save, 5000);
}

function setModified() {
    this.modified = true;
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
