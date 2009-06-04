// Add table-tr hover support to IE6 browsers

$(document).ready(function() {
  if($.browser.msie && /6.0/.test(navigator.userAgent)){
    $("table.directoryListing tr").hover(
      function() {
        $(this).toggleClass('hover');
      },
      function() {
        $(this).toggleClass('hover');
      }
    );
  }
});
