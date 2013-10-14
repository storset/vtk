/*
 * Vortex Simple Gallery jQuery plugin
 * w/ paging, centered thumbnail navigation and crossfade effect (dimensions from server)
 *
 * Copyright (C) 2010- Øyvind Hatland - University Of Oslo / USIT
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

(function ($) {
  $.fn.vrtxSGallery = function (wrapper, container, maxWidth, options) {
    settings = jQuery.extend({ // Default animation settings
      fadeInOutTime: 250,
      fadedOutOpacity: 0,
      fadeThumbsInOutTime: 250,
      fadedThumbsOutOpacity: 0.6,
      fadeNavInOutTime: 250
    }, options || {});
    
    var wrp = $(wrapper);

    // Unobtrusive
    wrp.find(container + "-pure-css").addClass(container.substring(1));
    wrp.find(container + "-nav-pure-css").addClass(container.substring(1) + "-nav");
    wrp.find(wrapper + "-thumbs-pure-css").addClass(wrapper.substring(1) + "-thumbs");

    var wrapperContainer = wrapper + " " + container;
    var wrapperContainerLink = wrapperContainer + " a" + container + "-link";
    var wrapperThumbsLinks = wrapper + " li a";
    
    // Cache containers and image HTML with src as hash
    var wrpContainer = $(wrapperContainer);
    var wrpContainerLink = $(wrapperContainer + " a" + container + "-link");
    var wrpThumbsLinks = $(wrapperThumbsLinks);
    var images = {}; 

    // Init first active image
    var firstImage = wrpThumbsLinks.filter(".active");
    if(!firstImage.length) return this; 
    
    calculateImage(firstImage.find("img.vrtx-thumbnail-image"), true);
    wrp.find("a.prev, a.prev span, a.next, a.next span").fadeTo(0, 0);
    
    // Thumbs
    wrp.on("mouseover mouseout click", "li a", function (e) {
      var elm = $(this);
      if (e.type == "mouseover" || e.type == "mouseout") {
        elm.filter(":not(.active)").find("img").stop().fadeTo(settings.fadeThumbsInOutTime, (e.type == "mouseover") ? 1 : settings.fadedThumbsOutOpacity);
      } else {
        navigate(elm);
        e.stopPropagation();
        e.preventDefault();
      }
    });

    // Navigation handlers
    $(document).keydown(function (e) {
      if (e.keyCode == 39) {
        nextPrevNavigate(e, 1);
      } else if (e.keyCode == 37) {
        nextPrevNavigate(e, -1);
      }
    });
    wrp.on("click mouseover mouseout", "a.next, " + container + "-link", function (e) {
      nextPrevNavigate(e, 1);
    });

    wrp.on("click mouseover mouseout", "a.prev", function (e) {
      nextPrevNavigate(e, -1);
    });

    // Generate markup for rest of images
    var imgs = this,
        centerThumbnailImageFunc = centerThumbnailImage, 
        cacheGenerateLinkImageFunc = cacheGenerateLinkImage, link, image;
    for(var i = 0, len = imgs.length; i < len; i++) {
      link = $(imgs[i]);
      image = link.find("img.vrtx-thumbnail-image");
      centerThumbnailImageFunc(image, link);
      cacheGenerateLinkImageFunc(image.attr("src").split("?")[0], image, link);
    }
    
    // Prefetch next and prev full images in the background
    var imageUrlsToBePrefetchedLen = imageUrlsToBePrefetched.length - 1,
        imagesPrefetched = {}; // Keeps images in memory (reachable) so that don't need to prefetch again until reload

    var loadErrorFullImage = function(image) {
      // void() - could maybe be used for something later :)
    },
    loadFullImage = function() {
      loadErrorFullImage(this);
    },
    errorFullImage = function() {
      $(imgs).filter("[href^='" + this.src + "']").closest("a").append("<span class='loading-image loading-image-error'><p>" + loadImageErrorMsg + "</p></span>");
      loadErrorFullImage(this);
    },
    loadImage = function(src) {
      imagesPrefetched[src] = new Image();
      imagesPrefetched[src].onload = loadFullImage;
      imagesPrefetched[src].onerror = errorFullImage;
      imagesPrefetched[src].src = src;
    },
    prefetchNextPrev = function() {
      var active = wrpThumbsLinks.filter(".active"),
          activeIdx = active.parent().index() - 1,
          activeSrc = active.find(".vrtx-thumbnail-image")[0].src.split("?")[0],
          j = 0;

      if(!imagesPrefetched[activeSrc]) {
        loadImage(activeSrc);
      }   
          
      var loadNextPrevImages = setTimeout(function() {
        if(j === 0) {
          var imageUrlToBePrefetchedIdx = activeIdx + 1;
          if(imageUrlToBePrefetchedIdx > imageUrlsToBePrefetchedLen) {
            imageUrlToBePrefetchedIdx = 0;
          }
        } else {
          var imageUrlToBePrefetchedIdx = activeIdx - 1;
          if(imageUrlToBePrefetchedIdx < 0) {
            imageUrlToBePrefetchedIdx = imageUrlsToBePrefetchedLen;
          }
        }
        var src = imageUrlsToBePrefetched[imageUrlToBePrefetchedIdx];
        if(!imagesPrefetched[src]) {
          loadImage(src);
        }
        j++;
        if(j < 2) {
          setTimeout(arguments.callee, 20);
        }
      }, 20);
    };
    
    prefetchNextPrev();
  
    return imgs; /* Make chainable */
    
    function navigate(elm) {
      var img = elm.find("img.vrtx-thumbnail-image");
      calculateImage(img, false);
      elm.addClass("active");
      prefetchNextPrev();
      img.stop().fadeTo(0, 1);
    }
    
    function nextPrevNavigate(e, dir) {
      var isNext = dir > 0;	
      if (e.type == "mouseover") {
        wrp.find("a.next span, a.prev span").stop().fadeTo(settings.fadeNavInOutTime, 0.2);   /* XXX: some filtering instead below */
        wrp.find("a." + (isNext ? "next" : "prev")).stop().fadeTo(settings.fadeNavInOutTime, 1);
        wrp.find("a." + (isNext ? "prev" : "next")).stop().fadeTo(settings.fadeNavInOutTime, 0.5);
      } else if (e.type == "mouseout") {
        wrp.find("a.prev, a.prev span, a.next, a.next span").stop().fadeTo(settings.fadeNavInOutTime, 0);
      } else {
        var activeThumb = wrpThumbsLinks.filter(".active").parent();
        var elm = isNext ? activeThumb.next("li") : activeThumb.prev("li");
        if (elm.length) {
          navigate(elm.find("a"));
        } else {
          navigate(wrp.find("li").filter((isNext ? ":first" : ":last") + ":visible").find("a"));
        }
        if(!e.keyCode) {
          e.stopPropagation();
          e.preventDefault();
        }
      }
    }
    
    function calculateImage(image, init) {
      var src = image.attr("src").split("?")[0]; /* Remove parameters when active is sent in to gallery */
      if (settings.fadeInOutTime > 0 && !init) {
        wrpContainer.append("<div class='over'>" + $(wrapperContainerLink).html() + "</div>");
        $(wrapperContainerLink).remove();
        $(".over").fadeTo(settings.fadeInOutTime, settings.fadedOutOpacity, function () {
          $(this).remove();
        });
      } else {
        if (init) {
          cacheGenerateLinkImage(src, image, image.parent());
        } else {
          $(wrapperContainerLink).remove();
        }
      }
      wrpContainer.append(images[src].html);
      scaleAndCalculatePosition(src);
      if(!init) {
        wrpThumbsLinks.filter(".active").removeClass("active").find("img").stop().fadeTo(settings.fadeThumbsInOutTime, settings.fadedThumbsOutOpacity);
      } else {
        wrpThumbsLinks.filter(":not(.active)").find("img").stop().fadeTo(0, settings.fadedThumbsOutOpacity);
      }
    }

    function scaleAndCalculatePosition(src) {
      $(wrapperContainer + "-nav a, " + wrapperContainer + "-nav span, " + wrapperContainerLink).css("height", images[src].height);
      $(wrapperContainer + ", " + wrapperContainer + "-nav").css("width", images[src].width);
      
      var description = $(wrapperContainer + "-description");
      if(!description.length) {
        $("<div class='" + container.substring(1) + "-description' />").insertAfter(wrapperContainer);
        description = $(wrapperContainer + "-description");
      }
      description.html(images[src].desc).css("width", images[src].width);
    }

    function centerThumbnailImage(thumb, link) {
      centerDimension(thumb, thumb.width(), link.width(), "marginLeft"); // horizontal
      centerDimension(thumb, thumb.height(), link.height(), "marginTop"); // vertical
    }

    function centerDimension(thumb, thumbDimension, thumbContainerDimension, cssProperty) {
      var adjust = 0;
      if (thumbDimension > thumbContainerDimension) {
        adjust = ((thumbDimension - thumbContainerDimension) / 2) * -1;
      } else if (thumbDimension < thumbContainerDimension) {
        adjust = (thumbContainerDimension - thumbDimension) / 2;
      }
      thumb.css(cssProperty, adjust + "px");
    }

    function cacheGenerateLinkImage(src, image, link) {
      images[src] = {};
      images[src].width = parseInt(link.find("span.hiddenWidth").text(), 10);
      images[src].height = parseInt(link.find("span.hiddenHeight").text(), 10);
      // HTML encode quotes in alt and title if not already encoded
      var alt = image.attr("alt");
      var title = image.attr("title");
      images[src].alt = alt ? alt.replace(/\'/g, "&#39;").replace(/\"/g, "&quot;") : null;
      images[src].title = title ? title.replace(/\'/g, "&#39;").replace(/\"/g, "&quot;") : null;
      // Build HTML
      images[src].html = "<a href='" + link.attr("href") + "'" +
                         " class='" + container.substring(1) + "-link'>" +
                         "<img src='" + src + "' alt='" + images[src].alt + "' style='width: " +
                         images[src].width + "px; height: " + images[src].height + "px;' />" + "</a>";
      /* Minimum 150x100px containers */
      images[src].width = Math.max(parseInt(images[src].width, 10), 150) + "px";
      images[src].height = Math.max(parseInt(images[src].height, 10), 100) + "px";
      var desc = "";
      if (images[src].title) desc += "<p class='" + container.substring(1) + "-title'>" + images[src].title + "</p>";
      if (images[src].alt)   desc += images[src].alt;
      images[src].desc = desc;
    }
  };
})(jQuery);

/* ^ Vortex Simple Gallery jQuery plugin */