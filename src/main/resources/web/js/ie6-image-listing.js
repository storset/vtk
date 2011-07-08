// IE 6 max-height CSS substitute
$(document).ready(function () {
  if ($.browser.msie && $.browser.version <= 6) {
    var maxSize = parseInt($(this).css('height'));
    var imageContainers = $(".vrtx-image-container");
    for (var i = imageContainers.length; i--;) { // performance: two fewer operations per iteration
      var img = $(this).find("img"); // performance: cache object
      var height = img.height();
      if (height > maxSize) {
        img.css('height', maxSize + 'px');
      }
    }
  }
});