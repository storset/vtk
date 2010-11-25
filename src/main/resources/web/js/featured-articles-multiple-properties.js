
function loadFeaturedArticles(addName, removeName, browseName, editorBase, baseFolder, editorBrowseUrl) {
    if ($( "#resource\\.featured-articles" ).val() == null)
        return;

    $("#resource\\.featured-articles").hide();

    // TODO: use only one append
    $("#vrtx-resource\\.featured-articles").append("<div id='vrtx-featured-article-add'>" +
            "<button  onClick=\"addFormField(null, '"+ removeName + "', '" + browseName + "', '" + 
            editorBase + "', '" + baseFolder + "', '" + editorBrowseUrl + "'); return false;\">" + addName + "</button></div>");
    $("#vrtx-resource\\.featured-articles").append("<input type='hidden' id='id' name='id' value='1' />");

    var listOfFiles = document.getElementById("resource\.featured-articles").value.split(",");
    var listOfFilesLength = listOfFiles.length;
    var addFormFieldFunc = addFormField;
    for (var i = 0; i < listOfFilesLength; i++) {
        addFormFieldFunc(jQuery.trim(listOfFiles[i]), removeName, browseName, editorBase, baseFolder, editorBrowseUrl);
    }
}

function addFormField(value, removeName, browsName, editorBase, baseFolder, editorBrowseUrl) {
    var idstr = "vrtx-featured-articles-";
    var id = document.getElementById("id").value;
    if (value == null)
        value = "";

    var deleteRow;
    if (removeName == null) {
        deleteRow = "";
    } else {
        deleteRow = "<button type='button' id='" + idstr + "remove' onClick='removeFormField(\"#" + idstr + "row-" + id +
            "\"); return false;'>" + removeName + "</button>";
    }

    var browseServer = "<button type=\"button\" id=\"" + idstr + "browse\" onclick=\"browseServer('" + idstr + id + "', '" +
            editorBase + "', '" + baseFolder + "', '" + editorBrowseUrl + "', 'File');\">" + browsName + "</button>";
    var classStr = " class='"  + idstr + "style' ";

    $("<p " + classStr + " id='"+ idstr + "row-" + id + "'><input value='" + value +  "'type='text' size='20′ name='txt[]' id='" +
            idstr + id + "'> " + browseServer + deleteRow + "</p>").insertBefore("#vrtx-featured-article-add");

    id++;
    document.getElementById("id").value = id;
}

function removeFormField(id) {
    $(id).remove();
}

function formatFeaturedArticlesData() {
    if ($( "#resource\\.featured-articles" ).val() == null)
        return;

    var data = $.find("input[id^='vrtx-featured-articles-']");
    var dataLength = data.length;
    var result = "";
    for (var i = 0; i < dataLength; i++) {
        result += data[i].value;
        if (i < (dataLength-1)) {
            result += ",";
        }
    }
    document.getElementById("resource\.featured-articles").value = result;
}
