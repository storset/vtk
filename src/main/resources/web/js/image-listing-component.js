// Vortex Simple Gallery jQuery plugin v0.1b
// w/ paging, centered thumbnail navigation and fade effect
// by Øyvind Hatland - UiO / USIT

(function ($) {
  $.fn.vrtxSGallery = function (wrapper, container, maxWidth, options) {
	  
	  //cache images
	  var images = new Array();
	  
	  //default animation settings
	  settings = jQuery.extend({ 
		  fadeInOutTime : 250, 
		  fadedOutOpacity: 0, 
		  fadeThumbsInOutTime: 250,
		  fadedThumbsOutOpacity: 0.6,
		  fadeNavInOutTime: 250
		  },options||{});

	  //Unobtrusive JavaScript
	  $(container + "-pure-css").addClass(container.substring(1));
	  $(container + "-nav-pure-css").addClass(container.substring(1) + "-nav");
	  $(wrapper + "-thumbs-pure-css").addClass(wrapper.substring(1) + "-thumbs");

	  //paging (relative to li a.active)
	  addPagingClickAndHoverEvents("next");
	  addPagingClickAndHoverEvents("prev");

	  //init
	  calculateImage($(wrapper + " ul li a.active img"), 0, true);

	  return this.each(function (i) {
		   
		   var link = generateLinkImage($("img", this), $(this));
	       images[i] = link; //cache image
	       
		   $(this).hover(function () {
		    	if(!$(this).hasClass("active")) {
				 $("img", this).stop().fadeTo(settings.fadeThumbsInOutTime, 1);
		    	}
		    }, function () {
		    	if(!$(this).hasClass("active")) {
				 $("img", this).stop().fadeTo(settings.fadeThumbsInOutTime, settings.fadedThumbsOutOpacity);
		    	}
	       });
		   
		  $(this).click(function(e) {
			   calculateImage($("img", this), i, false)
	  	       $(this).addClass("active");
	  	       $("img", this).stop().fadeTo(0, 1);
			   e.preventDefault();
	      });
		  
		  centerThumbnailImage($("img", this)); 
	 });

	 function calculateImage(image, i, init) {
		  //replace link and image (w/ fade effect down to fadedOutOpacity) + stop() current animation.
	      if(settings.fadeInOutTime > 0 && !init) {
		      $(wrapper + " " + container).stop().fadeTo(settings.fadeInOutTime, settings.fadedOutOpacity, function() {
		    	  //done fade out -> remove
		    	  $("a" + container + "-link", this).remove();
		    	  //append image
		    	  $(this).append(images[i]); 
		    	  //fade in and make sure calculations are done after fully loaded
		    	  $(this).fadeTo(settings.fadeInOutTime, 1, function() {
		    		  addPagingClickAndHoverEvents(container + "-link");
		    		  calculateImageAndPagingNavigationPosition();
		    	  });
		      });
	      } else {
	    	  $(wrapper + " " + container + " a" + container + "-link").remove();
	    	  if(init) {
	    		var link = generateLinkImage($(image), $(image).parent());
	    		$(wrapper + " " + container).append(link);
	    	  } else {
	    	    $(wrapper + " " + container).append(images[i]);  
	    	  }
	    	  addPagingClickAndHoverEvents(container + "-link");
	    	  calculateImageAndPagingNavigationPosition();
	      }
	      //remove active classes
	      jQuery(wrapper + " ul li a").each(function(j) {
	    	if(jQuery(this).hasClass("active")) {
	    	   if(init) {
	    		 $(image).stop().fadeTo(0, 1);
	    	   } else {
	    	     jQuery(this).removeClass("active");
	    	     jQuery("img", this).stop().fadeTo(settings.fadeThumbsInOutTime, settings.fadedThumbsOutOpacity);
	    	   }
	    	} else {
	    	   jQuery("img", this).stop().fadeTo(0, settings.fadedThumbsOutOpacity);
	    	}
	      });
	  	  $(wrapper + " " + container + "-description").remove();
		  $("<div class='" + container.substring(1) + "-description'>" + $(image).attr("alt") + "</div>").insertAfter(wrapper + " " + container);
		  if($(image).attr("alt") != null && $(image).attr("alt") != "") {
		    $(wrapper + " " + container + "-description").css("width", $(wrapper + " " + container).width());
	  	  }
	 }
	  
	 function centerThumbnailImage(thumb) {
	   centerDimension($(thumb), $(thumb).width(), $(thumb).parent().width(), "marginLeft"); //center horizontal
	   centerDimension($(thumb), $(thumb).height(), $(thumb).parent().height(), "marginTop"); //center vertical
	 }
	 
	 function centerDimension(thumb, thumbDimension, thumbContainerDimension, cssProperty) {
	   if(thumbDimension > thumbContainerDimension) {
		 var adjust = (thumbDimension - thumbContainerDimension) / 2;
	     $(thumb).css(cssProperty, -adjust + "px"); 
	   } else if(thumbDimension < thumbContainerDimension) {
		 var adjust = ((thumbContainerDimension - thumbDimension) / 2);
		 $(thumb).css(cssProperty, adjust + "px");
	   }
	 }
	  
	 //TODO: refactor
	 function addPagingClickAndHoverEvents(navClass) {
	    $(wrapper + " " + " a." + navClass).click(function(e) {
	      if(navClass == "next" || navClass == container + "-link") {
			  if($(wrapper + " ul li a.active").parent().next().length != 0) {
			    $(wrapper + " ul li a.active").parent().next().find("a").click();
			  } else {
				$(wrapper + " ul li:first a").click();
			  }
	      } else {
	    	  if($(wrapper + " ul li a.active").parent().prev().length != 0) {
	    		$(wrapper + " ul li a.active").parent().prev().find("a").click();
	          } else {
	    		$(wrapper + " ul li:last a").click();   
	          }
	      }
		  e.preventDefault(); 
		});
	    //Fading of transparent block and prev / next icon
	    if(navClass == "next" || navClass == "prev") { 
	      fadeMultiple(new Array(wrapper + " " + " a." + navClass, wrapper + " " + " a." + navClass + " span"), 0, 0);
	      $(wrapper + " " + " a." + navClass).hover(function () {
			  $(wrapper + " " + " a." + navClass).stop().fadeTo(settings.fadeNavInOutTime, 1);
			  $(wrapper + " " + " a." + navClass + " span").stop().fadeTo(settings.fadeNavInOutTime, 0.2);
	      }, function () { //hover out
			  fadeMultiple(new Array(wrapper + " " + " a." + navClass, wrapper + " " + " a." + navClass + " span"), settings.fadeNavInOutTime, 0)
	      });
	    } else if (navClass == container + "-link") {
		  $("a" + container + "-link").hover(function () {
		    fadeMultiple(new Array(wrapper + " " + " a.prev", wrapper + " " + " a.next"), settings.fadeNavInOutTime, 1)
		    fadeMultiple(new Array(wrapper + " " + " a.next span", wrapper + " " + " a.prev span"), settings.fadeNavInOutTime, 0.2)
		  }, function () { //hover out
		    fadeMultiple(new Array(wrapper + " " + " a.next", wrapper + " " + " a.prev", 
						wrapper + " " + " a.next span", wrapper + " " + " a.prev span"), settings.fadeNavInOutTime, 0)
		  });
	    }
	 }
     
     function generateLinkImage(theimage, thelink) {
  	   var img = new Image();
  	   var src = $(theimage).attr("src").split("?")[0];
  	   var alt = $(theimage).attr("alt");
  	   img.src = src; img.alt = alt;
  	   link = document.createElement("a"); 
  	   link.setAttribute("href", $(thelink).attr("href"));
  	   link.setAttribute("class", container.substring(1) + "-link");
  	   link.setAttribute("className", container.substring(1) + "-link"); // IE
  	   $(link).append(img);
  	   return link;
  	 }

	 function calculateImageAndPagingNavigationPosition() {
	   var minHeight = 100; 
	   var minWidth = 250;
	   
	   //IE 6 max-width substitute
	   if (jQuery.browser.msie && jQuery.browser.version <= 6) {
	     var mainImgHeight = $("a" + container + "-link img").height();
	     if(mainImgHeight > 380) {
	       $("a" + container + "-link img").css("height", "380px");
	     }
	   }
		 
	   var imgHeight = $(wrapper + " " + container + " img").height();
	   var imgWidth = $(wrapper + " " + container + " img").width();
	   var containerWidth = $(wrapper + " " + container).width();
	   
	   var imgHeight = (imgHeight < minHeight) ? minHeight : imgHeight;

	   setMultipleCSS(new Array(wrapper + " " + container + "-nav a", wrapper + " " + container + "-nav span", 
			          wrapper + " " + container + "-link"), "height", imgHeight);

	   if(imgWidth > maxWidth) {
		 imgWidth = maxWidth;
	   } else if (imgWidth < minWidth) {
		 setMultipleCSS(new Array(container + "-link"), "width", imgWidth);
		 imgWidth = minWidth;
	   }
	   
	   setMultipleCSS(new Array(wrapper + " " + container, wrapper + " " + container + "-nav"), "width", imgWidth);  
	 }
	 
	 function setMultipleCSS(elements, cssProperty, value) {
	   for(var i = 0; i < elements.length; i++) {
		   $(elements[i]).css(cssProperty, value);
	   }
	 }
	 
	 function fadeMultiple(elements, time, opacity) {
	   for(var i = 0; i < elements.length; i++) {
	     $(elements[i]).stop().fadeTo(time, opacity);
	   }
	 }
	 
  };
})(jQuery);