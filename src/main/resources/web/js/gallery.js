/*
 * Vortex Simple Gallery jQuery plugin v0.3
 * w/ paging, centered thumbnail navigation and crossfade effect
 *
 * by �yvind Hatland - UiO / USIT
 *
 */

(function ($) {
  $.fn.vrtxSGallery = function (wrapper, container, maxWidth, options) {

    // Default animation settings
    settings = jQuery.extend({
      fadeInOutTime: 250,
      fadedOutOpacity: 0,
      fadeThumbsInOutTime: 250,
      fadedThumbsOutOpacity: 0.6,
      fadeNavInOutTime: 250
    }, options || {});

    // Unobtrusive JavaScript
    $(container + "-pure-css").addClass(container.substring(1));
    $(container + "-nav-pure-css").addClass(container.substring(1) + "-nav");
    $(wrapper + "-thumbs-pure-css").addClass(wrapper.substring(1) + "-thumbs");

    wrapperContainer = wrapper + " " + container;
    wrapperContainerLink = wrapperContainer + " a" + container + "-link";
    wrapperThumbsLinks = wrapper + " ul li a";

    var images = []; // cache image HTML with src as hash
    var imagesWidth = [];
    var imagesHeight = [];

    //Performance: function pointers to use inside loops
    var centerThumbnailImageFunc = centerThumbnailImage;
    var calculateImageFunc = calculateImage;
    var generateLinkImageFunc = generateLinkImage;
    
    // Init first active image
    calculateImageFunc($(wrapperThumbsLinks + ".active img.vrtx-thumbnail-image"), 
                       $(wrapperThumbsLinks + ".active img.vrtx-full-image"), true);

    initPagingEvents("prev");
    initPagingEvents("next");

    //TODO: use for- or async loop
    // Center thumbnails and cache images with link
    return this.each(function () {
      var link = $(this);
      var img = link.find("img.vrtx-full-image");
      var src = img.attr("src");
      imagesWidth[src] = parseInt(link.find("span.hiddenWidth").text());
      imagesHeight[src] = parseInt(link.find("span.hiddenHeight").text());
      images[src] = generateLinkImageFunc(img, link, false);
      centerThumbnailImageFunc(link.find("img.vrtx-thumbnail-image"), link);
    });
    
     // Event-handlers
    $(document).keydown(function (e) {
      if (e.keyCode == 37) {
        $(wrapper + " a.prev").click();
      } else if (e.keyCode == 39) {
        $(wrapper + " a.next").click();
      }
    });

    $(wrapper).on("mouseover mouseout click", "ul li a", function (e) {
      var h = $(this);
      if (e.type == "mouseover") {
        if (!h.hasClass("active")) {
          h.find("img").stop().fadeTo(settings.fadeThumbsInOutTime, 1);
        }
      } else if (e.type == "mouseout") {
        if (!h.hasClass("active")) {
          h.find("img").stop().fadeTo(settings.fadeThumbsInOutTime, settings.fadedThumbsOutOpacity);
        }
      } else {
        var img = h.find("img.vrtx-thumbnail-image");
        var fullImage = h.find("img.vrtx-full-image");
        calculateImageFunc(img, fullImage, false);
        h.addClass("active");
        img.stop().fadeTo(0, 1);
        e.preventDefault()
      }
    });

    $(wrapper).on("click mouseover mouseout", "a.next, " + container + "-link", function (e) {
      if (e.type == "mouseover") {
        fadeMultiple([wrapper + " a.next span",
                      wrapper + " a.prev span"], settings.fadeNavInOutTime, 0.2);
        $(wrapper + " a.next").stop().fadeTo(settings.fadeNavInOutTime, 1);
        $(wrapper + " a.prev").stop().fadeTo(settings.fadeNavInOutTime, 0.5);
      } else if (e.type == "mouseout") {
        fadeMultiple([wrapper + " a.next", wrapper + " a.next span",
                      wrapper + " a.prev", wrapper + " a.prev span"], settings.fadeNavInOutTime, 0);
      } else {
        var activeThumb = $(wrapperThumbsLinks + ".active");
        if (activeThumb.parent().next().length != 0) {
          activeThumb.parent().next().find("a").click();
        } else {
          $(wrapper + " ul li:first a").click();
        }
        e.preventDefault();
      }
    });

    $(wrapper).on("click mouseover mouseout", "a.prev", function (e) {
      if (e.type == "mouseover") {
        fadeMultiple([wrapper + " a.next span",
                                wrapper + " a.prev span"], settings.fadeNavInOutTime, 0.2);
        $(wrapper + " a.prev").stop().fadeTo(settings.fadeNavInOutTime, 1);
        $(wrapper + " a.next").stop().fadeTo(settings.fadeNavInOutTime, 0.5);
      } else if (e.type == "mouseout") {
        fadeMultiple([wrapper + " a.next", wrapper + " a.next span",
                      wrapper + " a.prev", wrapper + " a.prev span"], settings.fadeNavInOutTime, 0);
      } else {
        var activeThumb = $(wrapperThumbsLinks + ".active");
        if (activeThumb.parent().prev().length != 0) {
          activeThumb.parent().prev().find("a").click();
        } else {
          $(wrapper + " ul li:last a").click();
        }
        e.preventDefault();
      }
    });

    function calculateImage(image, fullImage, init) {
      if (settings.fadeInOutTime > 0 && !init) {
        $(wrapperContainer).append("<div class='over'>" + $(wrapperContainerLink).html() + "</div>");
        $(wrapperContainerLink).remove();
        $(wrapperContainer).append(images[fullImage.attr("src")]);
        scaleAndCalculatePosition(fullImage);
        $(".over").fadeTo(settings.fadeInOutTime, settings.fadedOutOpacity, function () {
          $(this).remove();
        });
      } else {
        if (init) {
          $(wrapperContainer).append(generateLinkImageFunc($(fullImage), $(image).parent(), true));
        } else {
          $(wrapperContainerLink).remove();
          $(wrapperContainer).append(images[fullImage.attr("src")]);
        }
        scaleAndCalculatePosition(fullImage);
      }

      var thumbs = $(wrapperThumbsLinks);
      var thumbsLength = thumbs.length,
          i = 0,
          thumb;
      for (; i < thumbsLength; i++) {
        thumb = $(thumbs[i]);
        if (thumb.hasClass("active")) {
          if (!init) {
            thumb.removeClass("active");
            thumb.find("img").stop().fadeTo(settings.fadeThumbsInOutTime, settings.fadedThumbsOutOpacity);
          }
        } else {
          thumb.find("img").stop().fadeTo(0, settings.fadedThumbsOutOpacity);
        }
      }
    }

    function initPagingEvents(navClass) {
      fadeMultiple(new Array(wrapper + " a." + navClass, wrapper + " a." + navClass + " span"), 0, 0);
    }

    function scaleAndCalculatePosition(image, init) {
      var src = image.attr("src").split("?")[0];

      var imgWidth = imagesWidth[src];
      var imgHeight = imagesHeight[src];

      var minContainerHeight = 100;
      var minContainerWidth = 150;

      if (parseInt(imgHeight) < minContainerHeight) {
        imgHeight = minContainerHeight + "px";
      } else {
        imgHeight = imgHeight + "px";
      }
      if (parseInt(imgWidth) < minContainerWidth) {
        imgWidth = minContainerWidth + "px";
      } else {
        imgWidth = imgWidth + "px";
      }

      // For Debugging: galleryLog(src + " [" + imgWidth + ", " + imgHeight + "]");

      setMultipleCSS([wrapperContainer + "-nav a", wrapperContainer + "-nav span",
                               wrapperContainerLink], "height", imgHeight);

      setMultipleCSS([wrapperContainer, wrapperContainer + "-nav"], "width", imgWidth);

      $(wrapperContainer + "-description").remove();

      var html = "<div class='" + container.substring(1) + "-description'>";
      if ($(image).attr("title") && $(image).attr("title") != "") {
        html += "<p class='" + container.substring(1) + "-title'>" + $(image).attr("title") + "</p>";
      }
      if ($(image).attr("alt") && $(image).attr("alt") != "") {
        html += $(image).attr("alt");
      }
      html += "</div>";
      $(html).insertAfter(wrapperContainer);

      if (($(image).attr("alt") && $(image).attr("alt") != "") 
       || ($(image).attr("title") && $(image).attr("title") != "")) {
        $(wrapperContainer + "-description").css("width", imgWidth);
      }
    }

    function galleryLog(msg) {
      if (typeof console != "undefined" && console.log) {
        console.log(msg);
      }
    }

    function centerThumbnailImage(thumb, link) {
      centerDimension(thumb, thumb.width(), link.width(), "marginLeft"); // horizontal
      centerDimension(thumb, thumb.height(), link.height(), "marginTop"); // vertical
    }

    function centerDimension(thumb, thumbDimension, thumbContainerDimension, cssProperty) {
      if (thumbDimension > thumbContainerDimension) {
        var adjust = (thumbDimension - thumbContainerDimension) / 2;
        $(thumb).css(cssProperty, -adjust + "px");
      } else if (thumbDimension < thumbContainerDimension) {
        var adjust = (thumbContainerDimension - thumbDimension) / 2;
        $(thumb).css(cssProperty, adjust + "px");
      }
    }

    function generateLinkImage(theimage, thelink, init) {
      var src = theimage.attr("src").split("?")[0];
      var alt = theimage.attr("alt");
      if (!init) {
        var width = imagesWidth[src];
        var height = imagesHeight[src];
      } else {
        var width = parseInt(thelink.find("span.hiddenWidth").text());
        var height = parseInt(thelink.find("span.hiddenHeight").text());
        imagesWidth[src] = width;
        imagesHeight[src] = height;
      }

      return "<a href='" + $(thelink).attr("href") + "'" 
           + " class='" + container.substring(1) + "-link'>"
           + "<img src='" + src + "' alt='" + alt + "' style='width: "
           + width + "px; height: " + height + "px;' />" + "</a>";
    }

    function setMultipleCSS(elements, cssProperty, value) {
      var elementsLength = elements.length;
      for (var i = 0; i < elementsLength; i++) {
        $(elements[i]).css(cssProperty, value);
      }
    }

    function fadeMultiple(elements, time, opacity) {
      var elementsLength = elements.length;
      for (var i = 0; i < elementsLength; i++) {
        $(elements[i]).stop().fadeTo(time, opacity);
      }
    }
  };
})(jQuery)

/* ^ Vortex Simple Gallery jQuery plugin */