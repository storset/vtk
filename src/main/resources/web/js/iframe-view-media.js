/*  Based on code found on the web page "http://sonspring.com/journal/jquery-iframe-sizing" which 
 *  was written by Nathan Smith (http://technorati.com/people/technorati/nathansmith/)
 *
 *  Changed to only process specific frame and pass height to parent with postMessage.
 *  Should work as before with regard to document.body (served from the view domain). 
 *  Resizing the outer iframe (served from the admin domain) only works on browsers which support postMessage.
 */
 
var crossDocComLink = new CrossDocComLink();
 
$(document).ready(function () {
  $(window).load(function (e) {  // Set inline style to equal the body height of the iframed content,                     
    var setHeight = 350;         // when body content is at least 350px height
    var computedHeight = document.body.offsetHeight;
    if (computedHeight > setHeight) {
      setHeight = computedHeight;
    }
    document.body.style.height = setHeight + "px"; 
    
    // Pass our height to parent since it is typically cross domain (and can't access it directly)
    crossDocComLink.postCmdToParent("preview-height|" + setHeight);
    
    for (var i = 0, links = $("a"), len = links.length; i < len; i++) {
      $(links[i]).attr("target", "_top");
    }
  });
});