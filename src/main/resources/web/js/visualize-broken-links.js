/* 
 * Visualize broken links
 *
 */

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
  var links = context.contents().find("a." + linkClass);
  for (var i = 0, linksLength = links.length; i < linksLength; i++) {
    if (urls[idx].length == chunk) {
      idx++;
    }
    if (!urls[idx]) {
      urls[idx] = [];
    }
    var list = urls[idx];
    var href = $(links[i]).attr("href");
    list[list.length] = href;
  }
  var urlsLength = urls.length;
  if (urlsLength == 0) {
    if (options.completed) options.completed(0);
    return;
  }
  var reqs = 0;
  var brokenLinks = 0;
  $.each(urls, function (k, list) {
    var data = "";
    for (var j = 0, listLength = list.length; j < listLength; j++) {
      data += list[j] + "\n";
    }
    $.ajax({
      type: 'POST',
      url: validationURL,
      data: data,
      contentType: 'text/plain;charset=utf-8',
      dataType: 'json',
      context: context,
      success: function (results, status, resp) {
        brokenLinks += linkCheckResponse(results, $(this), options.responseLocalizer, linkClass);
      },
      complete: function (req, status) {
        reqs++;
        if (reqs == urlsLength && options.completed) {
          options.completed(reqs, brokenLinks);
        }
      }
    });
  });
}

function linkCheckResponse(results, context, localizer, linkClass) {
  var links = context.contents().find("a." + linkClass);
  var brokenLinks = 0;
  for (var j = 0, linksLength = links.length; j < linksLength; j++) {
    var href = $(links[j]).attr('href');
    for (var i = 0, resultsLength = results.length; i < resultsLength; i++) {
      if (results[i].status != "OK") {
        if (href == results[i].link) {
          if (results[i].status == "NOT_FOUND") {
            var color = "red";
            brokenLinks++;
          } else {
            var color = "brown";
            if (results[i].status == "ERROR") {
              brokenLinks++;
            }
          }
          var msg = (localizer) ? localizer(results[i].status) : results[i].status;
          $(links[j]).append(" - [" + msg + "]").css("color", color).removeClass(linkClass);
          break;
        }
      }
    }
  }
  return brokenLinks;
}

/* ^ Visualize broken links */
