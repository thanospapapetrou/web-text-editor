function browse() {
    const request = new XMLHttpRequest();
    request.onload = () => {
        const files = JSON.parse(this.response).files;
        if (files.length) {
            const list = document.createElement('ul');
            files.forEach(file => {
                const element = document.createElement('li');
                const link = document.createElement('a');
                link.href = '/edit.html?file=' + file;
                link.appendChild(document.createTextNode(file));
                element.appendChild(link);
                list.appendChild(element);
            });
            document.body.appendChild(list);
        } else {
            const noFiles = document.createElement('p');
            noFiles.appendChild(document.createTextNode('No files found'));
            document.body.appendChild(noFiles);
        }
    }
    request.open("GET", "/files", true);
    request.send();
}
