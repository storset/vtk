
function previewImage(urlobj) {
    var previewobj = urlobj + '.preview';
    if (document.getElementById(previewobj)) {
        var url = document.getElementById(urlobj).value;
        if (url) {
            document.getElementById(previewobj).innerHTML = 
                '<img src="' + url + '?vrtx=thumbnail" alt="preview">';
        } else {
            document.getElementById(previewobj).innerHTML = 
                '<img src=""  alt="no-image" style="visibility: hidden; width: 10px;">';
        }
    }
}