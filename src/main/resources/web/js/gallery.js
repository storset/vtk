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
      fadeInOutTime: 250, fadedOutOpacity: 0,
      fadeThumbsInOutTime: 250, fadedThumbsOutOpacity: 0.6,
      fadeNavInOutTime: 250, fadedInActiveNavOpacity: 0.5,
      fadedNavOpacity: 0.2, loadNextPrevImagesInterval: 20
    }, options || {});
    
    var wrp = $(wrapper);

    // Unobtrusive
    wrp.find(container + "-pure-css").addClass(container.substring(1));
    wrp.find(container + "-nav-pure-css").addClass(container.substring(1) + "-nav");
    wrp.find(wrapper + "-thumbs-pure-css").addClass(wrapper.substring(1) + "-thumbs");
    
    // Cache containers and image HTML with src as hash
    var wrapperContainer = wrapper + " " + container;
    var wrapperContainerLink = wrapperContainer + " a" + container + "-link",
        wrpContainer = $(wrapperContainer), wrpContainerLink = $(wrapperContainer + " a" + container + "-link"),
        wrpThumbsLinks = $(wrapper + " li a"), wrpNav = $(container + "-nav"),
        wrpNavNextPrev = wrpNav.find("a"), wrpNavNext = wrpNavNextPrev.filter(".next"),
        wrpNavPrev = wrpNavNextPrev.filter(".prev"), wrpNavNextPrevSpans = wrpNavNextPrev.find("span"),
        images = {}, imageUrlsToBePrefetchedLen = imageUrlsToBePrefetched.length - 1, isFullscreen = false,
        widthProp = "width", heightProp = "height";
    
    // Init first active image
    var firstImage = wrpThumbsLinks.filter(".active");
    if(!firstImage.length) return this; 
    showImage(firstImage.find("img.vrtx-thumbnail-image"), true);
    $(wrpNavNextPrev, wrpNavNextPrevSpans).fadeTo(0, 0);
    
    // Thumbs interaction
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

    // Navigation interaction
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
    
    // Fullscreen toggle interaction
    wrp.on("click", "a.toggle-fullscreen", function (e) {
      $("html").toggleClass("fullscreen-gallery");
      isFullscreen = $("html").hasClass("fullscreen-gallery");
      wrp.parents().toggleClass("fullwidth").toggle();
      if(!isFullscreen) {
        widthProp = "width";
        heightProp = "height";
        resizeToggleFullscreen();
      } else {
        widthProp = "fullWidth";
        heightProp = "fullHeight";
        if(!wrp.find("> .fullscreen-gallery-topline").length) {
          var link = $(this);
          wrp.prepend("<div class='fullscreen-gallery-topline'><a href='javascript:void(0);' class='toggle-fullscreen'>" + closeFullscreen + "</a></div>");
        }
        window.scrollTo(0, 0);
        resizeFullscreen();
      }    
      e.stopPropagation();
      e.preventDefault();
    });
    
    // Fullscreen resize
    $(window).resize($.throttle(250, function () {
      if(isFullscreen) {
        resizeFullscreen();
      }
    }));
    $(window).bind('orientationchange', function () {
      resizeFullscreen();
    });
    $(document).on('triggerResponsiveDesign', function(e, eventInfo) { 
      resizeFullscreen();
    });

    // Generate markup for rest of images
    var imgs = this, centerThumbnailImageFunc = centerThumbnailImage, 
        cacheGenerateLinkImageFunc = cacheGenerateLinkImage, link2, image2;
    for(var j = 0, len2 = imgs.length; j < len2; j++) {
      link2 = $(imgs[j]);
      image2 = link2.find("img.vrtx-thumbnail-image");
      centerThumbnailImageFunc(image2, link2);
      cacheGenerateLinkImageFunc(image2.attr("src").split("?")[0], image2, link2);
    }
    
    // Prefetch current, next and prev full images in the background
    prefetchCurrentNextPrevImage();
    
    wrp.removeClass("loading");
  
    return imgs; /* Make chainable */
    
    
    function navigate(elm) {
      var img = elm.find("img.vrtx-thumbnail-image");
      showImage(img, false);
      elm.addClass("active");
      prefetchCurrentNextPrevImage();
      img.stop().fadeTo(0, 1);
    }
    
    function nextPrevNavigate(e, dir) {
      var isNext = dir > 0;	
      if (e.type == "mouseover") {
        wrpNavNextPrevSpans.stop().fadeTo(settings.fadeNavInOutTime, settings.fadedNavOpacity);   /* XXX: some filtering instead below */
        if(isNext) {
          wrpNavNext.stop().fadeTo(settings.fadeNavInOutTime, 1);
          wrpNavPrev.stop().fadeTo(settings.fadeNavInOutTime, settings.fadedInActiveNavOpacity);
        } else {
          wrpNavPrev.stop().fadeTo(settings.fadeNavInOutTime, 1);
          wrpNavNext.stop().fadeTo(settings.fadeNavInOutTime, settings.fadedInActiveNavOpacity);
        }
      } else if (e.type == "mouseout") {
        $(wrpNavNextPrev, wrpNavNextPrevSpans).stop().fadeTo(settings.fadeNavInOutTime, 0);
      } else {
        var activeThumb = wrpThumbsLinks.filter(".active").parent();
        var elm = isNext ? activeThumb.next("li") : activeThumb.prev("li");
        if (elm.length) {
          navigate(elm.find("a"));
        } else {
          navigate(wrp.find("li").filter(isNext ? ":first" : ":last").find("a"));
        }
        if(!e.keyCode) {
          e.stopPropagation();
          e.preventDefault();
        }
      }
    }
    
    function loadImage(src) {
      var id = encodeURIComponent(src).replace(/(%|\.)/gim, "");
      if($("a#" + id).length) return;
      var description = "<div id='" + id + "-description' class='" + container.substring(1) + "-description" + (!images[src].desc ? " empty-description" : "") + "' style='display: none; width: " + (images[src].width - 30) + "px'>" +
                          "<a href='javascript:void(0);' class='toggle-fullscreen minimized'>" + showFullscreen + "</a>" + images[src].desc
                      + "</div>";
      $($.parseHTML(description)).insertBefore(wrapper + "-thumbs");
      wrpContainer.append("<a id='" + id + "' style='display: none' href='" + src + "' class='" + container.substring(1) + "-link'>" +
                            "<img src='" + src + "' alt='" + images[src].alt + "' style='width: " + images[src][widthProp] + "px; height: " + images[src][heightProp] + "px;' />" +
                          "</a>");
    }
    
    function prefetchCurrentNextPrevImage() {
      var active = wrpThumbsLinks.filter(".active"),
          activeIdx = active.parent().index() - 1,
          activeSrc = active.find(".vrtx-thumbnail-image")[0].src.split("?")[0],
          i = 0;
      loadImage(activeSrc);
      var loadNextPrevImages = setTimeout(function() {
        var activeIdxPlus1 = activeIdx + 1, activeIdxMinus1 = activeIdx - 1;
        var src = imageUrlsToBePrefetched[(i === 0) ? ( activeIdxPlus1 > imageUrlsToBePrefetchedLen ? 0 :  activeIdxPlus1)   // Next, first, prev or last
                                                    : (activeIdxMinus1 < 0 ? imageUrlsToBePrefetchedLen : activeIdxMinus1)].url;
        loadImage(src);
        if(++i < 2) {
          setTimeout(arguments.callee, settings.loadNextPrevImagesInterval);
        }
      }, settings.loadNextPrevImagesInterval);
    }

    function showImageCrossFade(current, active) {
      current.wrap("<div class='over' />").fadeTo(settings.fadeInOutTime, settings.fadedOutOpacity, function () {
        $(this).unwrap().removeClass("active-full-image").hide();
      });
      active.addClass("active-full-image").fadeTo(0, 0).fadeTo(settings.fadeInOutTime, 1);
    }
    
    function showImageToggle(current, active) {
      current.removeClass("active-full-image");
      active.addClass("active-full-image");
    }
    
    function showImageDescStrategy(current, active, currentDesc, activeDesc, init) {
      currentDesc.removeClass("active-description");
      activeDesc.addClass("active-description");
      if(init) {
        active.addClass("active-full-image");
      } else if(settings.fadeInOutTime > 0 ) {
        howImageCrossFade(current, active);
      } else {
        showImageToggle(current, active);
      }
    }
    
    function showImage(image, init) {
      var src = image.attr("src").split("?")[0]; /* Remove parameters when active is sent in to gallery */

      if (init) {
        cacheGenerateLinkImage(src, image, image.parent());
      }
      var activeId = encodeURIComponent(src).replace(/(%|\.)/gim, "");
      
      var active = $("a#" + activeId);
      var current = $("a" + container + "-link.active-full-image");
      var currentDesc = $(container + "-description.active-description");
      if(active.length) {
        showImageDescStrategy(current, active, currentDesc, $("#" + activeId + "-description"), init);
      } else {
        var waitForActive = setTimeout(function() {
          active = $("a#" + activeId);
          if(!active.length) {
            setTimeout(arguments.callee, 5);
          } else {
            showImageDescStrategy(current, active, currentDesc, $("#" + activeId + "-description"), init);
          }
        }, 5);
      }
      resizeContainers(src);
      if(!init) {
        wrpThumbsLinks.filter(".active").removeClass("active").find("img").stop().fadeTo(settings.fadeThumbsInOutTime, settings.fadedThumbsOutOpacity);
      } else {
        wrpThumbsLinks.filter(":not(.active)").find("img").stop().fadeTo(0, settings.fadedThumbsOutOpacity);
      }
    }

    function cacheGenerateLinkImage(src, image, link) {
      images[src] = {};
      // Find image width and height "precalculated" from Vortex (properties)
      for(var i = 0, len = imageUrlsToBePrefetched.length; i < len; i++) {
        var dims = imageUrlsToBePrefetched[i];
        if(dims.url === src) break;
      }
      images[src].width = parseInt(dims.width, 10);
      images[src].height = parseInt(dims.height, 10);
      images[src].fullWidthOrig = parseInt(dims.fullWidth.replace(/[^\d]*/g, ""), 10);
      images[src].fullHeightOrig = parseInt(dims.fullHeight.replace(/[^\d]*/g, ""), 10);
      // HTML encode quotes in alt and title if not already encoded
      var alt = image.attr("alt");
      var title = image.attr("title");
      images[src].alt = alt ? alt.replace(/\'/g, "&#39;").replace(/\"/g, "&quot;") : null;
      images[src].title = title ? title.replace(/\'/g, "&#39;").replace(/\"/g, "&quot;") : null;
      // Add description
      var desc = "";
      if (images[src].title) desc += "<p class='" + container.substring(1) + "-title'>" + images[src].title + "</p>";
      if (images[src].alt)   desc += images[src].alt;
      images[src].desc = desc;
    }
    
    function resizeContainers(src) {
      // Min 150x100px containers
      var width = Math.max(images[src][widthProp], 150);
      var height = Math.max(images[src][heightProp], 100);
      $(wrapperContainerLink).css("height", height + "px");
      $(wrpNavNextPrev, wrpNavNextPrevSpans).css("height", height + "px");
      wrpNav.css("width", width + "px");
      wrpContainer.css("width", width + "px");
      if(!isFullscreen) {
        $(wrapperContainer + "-description").css("width", (width - 30)); 
      }
    }
    
    function resizeToggleFullscreen() {
      var loadedImages = $("a" + container + "-link img");
      var src = $("a" + container + "-link.active-full-image")[0].href;
      for(var i = 0, len = loadedImages.length; i < len; i++) {
        loadedImages[i].style.width = images[loadedImages[i].src][widthProp] + "px";
        loadedImages[i].style.height = images[loadedImages[i].src][heightProp] + "px";
      }
      resizeContainers(src);
    }
    
    function resizeFullscreen() {
      for(var key in images) {
        var image = images[key];
        var dimsFull = calculateFullscreenImageDimensions(image.fullWidthOrig, image.fullHeightOrig, encodeURIComponent(key).replace(/(%|\.)/gim, ""));
        image.fullWidth = dimsFull[0];
        image.fullHeight = dimsFull[1];
      }
      resizeToggleFullscreen();
    }
  
    function calculateFullscreenImageDimensions(w, h, id) {
      var gcdVal = gcd(w, h);
      var aspectRatioOver = w/gcdVal;
      var aspectRatioUnder = h/gcdVal;
      var winWidth = $(window).width();
      var desc = $("#" + id + "-description");
      var descHeight = !desc.hasClass("empty-description") ? desc.outerHeight(true) : 0;
      var winHeight = $(window).height() - (descHeight + $(".fullscreen-gallery-topline").outerHeight(true)) - 20;
      if(w > winWidth || h > winHeight) {
        if(h > winHeight) {
          var newDim = [Math.round(winHeight * (aspectRatioOver / aspectRatioUnder)), winHeight];
          if(newDim[0] > winWidth) {
            var newDim = [winWidth, Math.round(winWidth / (aspectRatioOver / aspectRatioUnder))];
          }
        } else {
          var newDim = [winWidth, Math.round(winWidth / (aspectRatioOver / aspectRatioUnder))];
          if(newDim[1] > winHeight) {
            var newDim = [Math.round(winHeight * (aspectRatioOver / aspectRatioUnder)), winHeight];
          }
        }
        return [newDim[0], newDim[1]];
      } else {
        return [w, h];
      }
    }
    
    function gcd(a, b) {
      return (b === 0) ? a : gcd (b, a%b);
    }

    function centerThumbnailImage(thumb, link) {
      centerDimension(thumb, thumb.width(), link.width(), "marginLeft"); // Horizontal dimension
      centerDimension(thumb, thumb.height(), link.height(), "marginTop"); // Vertical dimension
    }

    function centerDimension(thumb, tDim, tCDim, cssProperty) { // Center thumbDimension in thumbContainerDimension
      thumb.css(cssProperty, ((tDim > tCDim) ? ((tDim - tCDim) / 2) * -1 : (tDim < tCDim) ? (tCDim - tDim) / 2 : 0) + "px");
    }
  };
})(jQuery);

/*
 * jQuery throttle / debounce - v1.1 - 3/7/2010
 * http://benalman.com/projects/jquery-throttle-debounce-plugin/
 * 
 * Copyright (c) 2010 "Cowboy" Ben Alman
 * Dual licensed under the MIT and GPL licenses.
 * http://benalman.com/about/license/
 */
(function(b,c){var $=b.jQuery||b.Cowboy||(b.Cowboy={}),a;$.throttle=a=function(e,f,j,i){var h,d=0;if(typeof f!=="boolean"){i=j;j=f;f=c}function g(){var o=this,m=+new Date()-d,n=arguments;function l(){d=+new Date();j.apply(o,n)}function k(){h=c}if(i&&!h){l()}h&&clearTimeout(h);if(i===c&&m>e){l()}else{if(f!==true){h=setTimeout(i?k:l,i===c?e-m:e)}}}if($.guid){g.guid=j.guid=j.guid||$.guid++}return g};$.debounce=function(d,e,f){return f===c?a(d,e,false):a(d,f,e!==false)}})(this);

/* ^ Vortex Simple Gallery jQuery plugin */