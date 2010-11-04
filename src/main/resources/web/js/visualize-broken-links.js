
function visualizeBrokenLinks(options) {
    if (!options) throw "Missing argument";
    if (!options.validationURL) throw "Missing 'validationURL' field in argument";
    if (!options.selection) throw "Missing 'selection' field in argument";
    var selection = options.selection;
    var validationURL = options.validationURL;
    var linkClass = options.linkClass ? options.linkClass : 'vrtx-link-check';
    var chunk = options.chunk ? options.chunk : 10;
    var urls = [];
    var idx = 0;
    urls[idx] = [];
    var context = $(selection);
    context.contents().find("a." + linkClass).each(function(elem) {
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

    if (urls.length == 0) {
        if (options.completed) options.completed(0);
        return;
    }
    var reqs = 0;
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
            context : context,
            success : function(results, status, resp) {
                return linkCheckResponse(results, $(this), options.localizer, linkClass);
            },
            complete : function(req, status) {
                reqs++;
                if (reqs == urls.length && options.completed) {
                    options.completed(reqs);
                }
            }
        });
    });
}

function linkCheckResponse(results, context, localizer, linkClass) {
    context.contents().find("a." + linkClass).each(function(e) {
        var href = $(this).attr('href');
        for (var i = 0; i < results.length; i++) {
            if (results[i].status != "OK") {
                if (href == results[i].link) {
                    var color = (results[i].status == "NOT_FOUND") ? "red" : "brown";
                    var msg = (localizer) ? localizer(results[i].status) : results[i].status;
                    $(this).append(" - [" + msg + "]").css("color", color).removeClass(linkClass);
                    break;
                }
            }
        }
    });
}
