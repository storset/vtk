$(document).ready( function() {
  //only affects IE 6 as max-width
  if (jQuery.browser.msie && jQuery.browser.version <= 6) {
    $(".vrtx-image-container").each( function(i) {

      var height = $(this).find("img").height();
      var maxSize = parseInt($(this).css('height'));

      if (height > maxSize) {
        $(this).find("img").css('height', maxSize + 'px');
      }

    });
  }
});
