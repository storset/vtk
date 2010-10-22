
function visualizeBrokenLinks(selection, validationURL, chunk) {
    var urls = [];
    var idx = 0;
    urls[idx] = [];
    var context = $(selection);
    context.contents().find("a.vrtx-link-check").each(function(elem) {
        if (urls[idx].length == chunk) {
            idx++;
        }
        if (!urls[idx]) {
            urls[idx] = [];
        }
        var list = urls[idx];
        var href = $(this).attr("href");
        list[list.length] = $(this).attr("href");
    });

    $.each(urls, function(i, list) {
        var data = "";
        for (var j = 0; j < list.length; j++) {
            data += list[j] + "\n";
        }
        $.ajax({
            type : 'POST',
            url : validationURL,
            data : data,
            dataType : 'text',
            contentType : 'text/plain',
            success : linkCheckResponse,
            context : context
        });
    });
}

function linkCheckResponse(data, status, resp) {
    var deadLinks = data.split("\n");
    $(this).contents().find("a.vrtx-link-check").each(function(e) {
        var href = $(this).attr('href');
        for (var i = 0; i < deadLinks.length; i++) {
            if (href == deadLinks[i]) {
                $(this).append(" - 404").css("color", "red").removeClass("vrtx-link-check");
                break;
            }
        }
    });
}
