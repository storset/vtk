/*  Based on code found on the web page "http://sonspring.com/journal/jquery-iframe-sizing" which
 *  was written by Nathan Smith (http://technorati.com/people/technorati/nathansmith/)
 *
 *  Changed to only process specific frame and pass height to parent with postMessage.
 *  Should work as before with regard to the previewViewIframe (served from the view domain). 
 *  Resizing the outer iframe (served from the admin domain) only works on browsers which support postMessage.
 *
 *  Updated with cross-browser postMessage: http://benalman.com/code/projects/jquery-postmessage/examples/iframe/
 */
$(document).ready(function () {

  if ($.browser.msie) {
    // iframe load event not firing in IE8 when page w. iframe is inside another iframe
    // Setting the iframe src seems to fix the problem
    var previewViewIframe = $("iframe#previewViewIframe")[0];
    if (previewViewIframe) {
      var iSource = previewViewIframe.src;
      previewViewIframe.src = '';
      previewViewIframe.src = iSource;
    }
  }
  $('iframe#previewViewIframe').load(function (e) {
    try {
      // Set inline style to equal the body height of the iframed content,
      // when body content is at least 350px height
      var setHeight = 350;
      var computedHeight = this.contentWindow.document.body.offsetHeight + 45;
      if (computedHeight > setHeight) {
        setHeight = computedHeight;
      }
      this.style.height = setHeight + "px";
      if (parent) {
        // Pass our height to parent since it is typically cross domain (and can't access it directly)
        $.postMessage({height: setHeight}, location.href, parent);
      }
    } catch(e){
      if(typeof console !== "undefined" && console.log) {
        console.log("Error in getting iframe height or trying to post it to parent: " + e.message);
      }
    }
  });
});