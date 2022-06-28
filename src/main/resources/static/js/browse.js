function browse() {
    const request = new XMLHttpRequest();
    request.onload = render;
    request.open('GET', '/files', true);
    request.send();
}

function render () {
    const files = JSON.parse(this.response).files;
    if (files.length) {
        const list = document.createElement('ul');
        files.forEach(renderFile);
        document.body.appendChild(list);
    } else {
        const noFiles = document.createElement('p');
        noFiles.appendChild(document.createTextNode('No files found'));
        document.body.appendChild(noFiles);
    }
}

function renderFile(file) {
    const item = document.createElement('li');
    const link = document.createElement('a');
    link.href = '/edit.html?file=' + file;
    link.appendChild(document.createTextNode(file));
    item.appendChild(link);
    list.appendChild(item);
}
