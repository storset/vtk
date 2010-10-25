
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
            contentType : 'text/plain',
            dataType : 'json',
            success : linkCheckResponse,
            context : context
        });
    });
}

function linkCheckResponse(results, status, resp) {
    $(this).contents().find("a.vrtx-link-check").each(function(e) {
        var href = $(this).attr('href');
        for (var i = 0; i < results.length; i++) {
            if (results[i].status != "OK") {
                if (href == results[i].link) {
                    var msg = "";
                    switch (results[i].status) {
                    case "NOT_FOUND":
                        msg = " [404]";
                        break;
                    case "TIMEOUT":
                        msg = " [timeout]";
                        break;
                    default:
                        msg = " [error]";
                    }
                    $(this).append(msg).css("color", "red").removeClass("vrtx-link-check");
                    break;
                }
            }
        }
    });
}
