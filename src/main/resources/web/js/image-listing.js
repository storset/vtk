
$(document).ready( function() {
  $(".vrtx-image-container").each( function(i) {

    var height = $(this).find("img").height();
    var maxSize = parseInt($(this).css('height'));

    if (height > maxSize) {
      $(this).find("img").css('height', maxSize + 'px');
    }

  });
});
