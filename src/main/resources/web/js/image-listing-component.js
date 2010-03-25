// Vortex Simple Gallery jQuery plugin v0.1b
// w/ paging, centered thumbnail navigation and fade effect
// by Øyvind Hatland - UiO / USIT

(function ($) {
  $.fn.vrtxSGallery = function (wrapper, container, options) {
	  
	  //cache images
	  var images = new Array();
	  
	  //default animation settings
	  settings = jQuery.extend({ 
		  fadeInOutTime : 250, 
		  fadedOutOpacity: 0, 
		  fadeThumbsInOutTime: 250,
		  fadedThumbsOutOpacity: 0.6,
		  fadeNavInOutTime: 250
		  }, 
	  options);

	  //Unobtrusive JavaScript
	  $(container + "-pure-css").addClass(container.substring(1));
	  $(container + "-nav-pure-css").addClass(container.substring(1) + "-nav");
	  $(wrapper + "-thumbs-pure-css").addClass(wrapper.substring(1) + "-thumbs");

	  initFirstImage();
	  
	  //paging (relative to li a.active)
	  addPagingClickEvents("next", wrapper);
	  addPagingClickEvents("prev", wrapper);
		  
	  return this.each(function (i) {
		   
		   var link = generateLinkImage($("img", this), $(this), container);
	      
	       //cache image
	       images[i] = link;
	       
		   $(this).hover(
		    function () {
		    	if(!$(this).hasClass("active")) {
				 $("img", this).stop().fadeTo(settings.fadeThumbsInOutTime, 1);
		    	}
		    }, //on hover out
		    function () {
		    	if(!$(this).hasClass("active")) {
				 $("img", this).stop().fadeTo(settings.fadeThumbsInOutTime, settings.fadedThumbsOutOpacity);
		    	}
	       });
		   
		   $(this).click(function(e) {
		  	  //replace link and image (w/ fade effect down to fadedOutOpacity) + stop() current animation.
		      if(settings.fadeInOutTime > 0) {
			      $(wrapper + " " + container).stop().fadeTo(settings.fadeInOutTime, settings.fadedOutOpacity, function() {
			    	  //done fade out -> remove
			    	  $("a" + container + "-link", this).remove();
			    	  //append image
			    	  $(this).append(images[i]); 
			    	  //fade in and make sure calculations are done after fully loaded
			    	  $(this).fadeTo(settings.fadeInOutTime, 1, function() {
			    		  addImageClickAndHoverEvents();
			    		  calculateImageAndPagingNavigationPosition();
			    	  });
			      });
		      } else {
		    	  $("a" + container + "-link", wrapper + " " + container).remove();
		    	  $(wrapper + " " + container).append(images[i]);
		    	  addImageClickAndHoverEvents();
		    	  calculateImageAndPagingNavigationPosition();
		      }
		      //remove active classes
		      jQuery(wrapper + " ul li a").each(function(j) {
		    	if(jQuery(this).hasClass("active")) {
		    	   jQuery(this).removeClass("active");
		    	   jQuery("img", this).stop().fadeTo(settings.fadeThumbsInOutTime, settings.fadedThumbsOutOpacity);
		    	} else {
		    	   jQuery("img", this).stop().fadeTo("0", settings.fadedThumbsOutOpacity);
		    	}
		      });
		      
		      //add new active class
		  	  $(this).addClass("active");
		  	  //make sure opacity is 1
		  	  $("img", this).stop().fadeTo("0", 1);

		  	  addDescription($("img", this));

			  e.preventDefault(); 
	      });
		  centerThumbnailImage($("img", this)); 
	 });
	  
	 function centerThumbnailImage(thumb) {
		 setTimeout(function() {
		   centerDimension($(thumb), $(thumb).width(), $(thumb).parent().width(), "marginLeft"); //center horizontal
		   centerDimension($(thumb), $(thumb).height(), $(thumb).parent().height(), "marginTop"); //center vertical
		 }, 100);
	 }
	 
	 function centerDimension(thumb, thumbDimension, thumbContainerDimension, cssProperty) {
	   var adjust = (thumbDimension - thumbContainer) / 2;
	   if(thumbDimension > thumbContainerDimension) {
	     $(thumb).css(cssProperty, -adjust + "px"); 
	   } else if(thumbDimension < thumbContainerDimension) {
		 $(thumb).css(cssProperty, adjust + "px");  
	   }
	 }
	  
	 function addPagingClickEvents(navClass, wrapper) {
	    $(wrapper + " " + " a." + navClass).click(function(h) {
	      if(navClass == "next") {
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
		  h.preventDefault(); 
		});
	    
	    //Fading of transparent block and prev / next icon
	    fadeMultiple(wrapper + " " + " a." + navClass, wrapper + " " + " a." + navClass + " span", "0", 0);
	    $(wrapper + " " + " a." + navClass).hover(function () {
			  $(wrapper + " " + " a." + navClass).stop().fadeTo(settings.fadeNavInOutTime, 1);
			  $(wrapper + " " + " a." + navClass + " span").stop().fadeTo(settings.fadeNavInOutTime, 0.2);
			}, function () { //hover out
			  fadeMultiple(new Array(wrapper + " " + " a." + navClass, wrapper + " " + " a." + navClass + " span"), settings.fadeNavInOutTime, 0)
	   });
	 }

	 function addImageClickAndHoverEvents() {
	   $("a" + container + "-link").click(function(e) { 
		  if($(wrapper + " ul li a.active").parent().next().length != 0) {
		    $(wrapper + " ul li a.active").parent().next().find("a").click();
		  } else {
			$(wrapper + " ul li:first a").click();
		  }
		  e.preventDefault(); 
	   });
	   $("a" + container + "-link").hover(function () {
			  fadeMultiple(new Array(wrapper + " " + " a.prev", wrapper + " " + " a.next"), settings.fadeNavInOutTime, 1)
		      fadeMultiple(new Array(wrapper + " " + " a.next span", wrapper + " " + " a.prev span"), settings.fadeNavInOutTime, 0.2)
	   }, function () { //hover out
			  fadeMultiple(new Array(wrapper + " " + " a.next", wrapper + " " + " a.prev", 
					                 wrapper + " " + " a.next span", wrapper + " " + " a.prev span"), settings.fadeNavInOutTime, 0)
	   });
	   
	   //IE 6 max-width substitute
	   if (jQuery.browser.msie && jQuery.browser.version <= 6) {
	     var mainImgHeight = $("a" + container + "-link img").height();
	     if(mainImgHeight > 380) {
		   $("a" + container + "-link img").css("height", "380px");
	     }
	   }
	 }

     function initFirstImage() {
		
	   var link = generateLinkImage(wrapper + " ul li a.active img", wrapper + " ul li a.active");
      
       $("a" + container + "-link", wrapper + " " + container).remove();
       $(wrapper + " " + container).append(link);
      
       addImageClickAndHoverEvents();
       calculateImageAndPagingNavigationPosition();
       addDescription(wrapper + " ul li a.active img");
	  
	   //set all thumbnails not active to settings.fadedThumbsOutOpacityopacity
	   jQuery(wrapper + " ul li a").each(function(j) {
		 if(!jQuery(this).hasClass("active")) {
		   jQuery("img", this).stop().fadeTo("0", settings.fadedThumbsOutOpacity);
		 }
	   });
	 }
     
     function generateLinkImage(theimage, thelink) {
       //create image
  	   var img = new Image();
  	   var src = $(theimage).attr("src").split("?")[0]; 
  	   var alt = $(theimage).attr("alt");
  	   img.src = src; img.alt = alt;
  	  
  	   //create link
  	   link = document.createElement("a"); 
  	   link.setAttribute("href", $(thelink).attr("href"));
  	   link.setAttribute("class", container.substring(1) + "-link");
  	   // IE
  	   link.setAttribute("className", container.substring(1) + "-link");
  	      
  	   //append img inside link
  	   $(link).append(img);

  	   return link;
  	 }
	  
	 function addDescription(fromSource) {
	   $(wrapper + " " + container + "-description").remove();
	   $("<div class='" + container.substring(1) + "-description'>" 
	          + $(fromSource).attr("alt") + "</div>").insertAfter(wrapper + " " + container);
	 }

	 function calculateImageAndPagingNavigationPosition() {
	   var minHeight = 100; 
	   var minWidth = 250;
		 
	   var imgHeight = $(wrapper + " " + container + " img").height();
	   var imgWidth = $(wrapper + " " + container + " img").width();
	   var containerWidth = $(wrapper + " " + container).width();
	   
	   var imgHeight = (imgHeight < minHeight) ? minHeight : imgHeight;

	   setMultipleCSS(new Array(wrapper + " " + container + "-nav a", wrapper + " " + container + "-nav span", 
			          wrapper + " " + container + "-link"), "height", imgHeight);

	   if(imgWidth > containerWidth) {
		 imgWidth = containerWidth;
	   } else if (imgWidth < 250) {
		 imgWidth = minWidth;
	   }
	   var leftRightNavAdjust = (containerWidth - imgWidth) / 2;
	   $(wrapper + " " + container + "-nav a.prev").css("left", leftRightNavAdjust);
	   $(wrapper + " " + container + "-nav a.next").css("right", -leftRightNavAdjust);
	   
	   setMultipleCSS(new Array(wrapper + " " + container + "-link", wrapper + " " + container + "-nav"), "width", imgWidth);
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