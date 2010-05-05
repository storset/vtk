
function previewImage(urlobj) {
    var previewobj = urlobj + '.preview';
    if (document.getElementById(previewobj)) {
        var url = document.getElementById(urlobj).value;
        if (url) {
            document.getElementById(previewobj).innerHTML = 
                '<img src="' + url + '?vrtx=thumbnail">';
        } else {
            document.getElementById(previewobj).innerHTML = '';
        }
    }
}