// IE 6 max-height CSS substitute

$(document).ready( function() {
  if (jQuery.browser.msie && jQuery.browser.version <= 6) {

    var maxSize = parseInt($(this).css('height'));

    $(".vrtx-image-container").each( function(i) {

      var height = $(this).find("img").height();

      if (height > maxSize) {
        $(this).find("img").css('height', maxSize + 'px');
      }

    });
  }
});
