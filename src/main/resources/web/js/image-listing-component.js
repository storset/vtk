// Vortex Simple Gallery jQuery plugin v0.1b
// w/ paging, centered thumbnail navigation and fade effect
// by �yvind Hatland - UiO / USIT

(function ($) {
  $.fn.vrtxSGallery = function (wrapper, container, maxWidth, options) {
	  
	  //cache images
	  var images = new Array();

	  //default animation settings
	  settings = jQuery.extend({ fadeInOutTime : 250, fadedOutOpacity: 0,
                                     fadeThumbsInOutTime: 250, fadedThumbsOutOpacity: 0.6,
                                     fadeNavInOutTime: 250}, options||{});

	  //Unobtrusive JavaScript
	  $(container + "-pure-css").addClass(container.substring(1));
	  $(container + "-nav-pure-css").addClass(container.substring(1) + "-nav");
	  $(wrapper + "-thumbs-pure-css").addClass(wrapper.substring(1) + "-thumbs");

	  wrapperContainer = wrapper + " " + container;
	  wrapperContainerLink = wrapperContainer + " a" + container + "-link";
	  wrapperThumbsLinks = wrapper + " ul li a";

	  //paging (relative to li a.active)
	  addPagingEvents("next");
	  addPagingEvents("prev");

	  //init first active image
	  calculateImage($(wrapperThumbsLinks + ".active img"), 0, true);

	  return this.each(function (i) {

               var link = generateLinkImage($("img", this), $(this));
	       images[i] = link; //cache image
                
               $(this).hover(function () { if(!$(this).hasClass("active")) { $("img", this).stop().fadeTo(settings.fadeThumbsInOutTime, 1); } },
                             function () { if(!$(this).hasClass("active")) { $("img", this).stop().fadeTo(settings.fadeThumbsInOutTime, settings.fadedThumbsOutOpacity); }
               });

               $(this).click(function(e) {
	               calculateImage($("img", this), i, false);
	  	       $(this).addClass("active");
	  	       $("img", this).stop().fadeTo(0, 1);
	               e.preventDefault();
               });

               centerThumbnailImage($("img", this));
	 });

	 function calculateImage(image, i, init) {
	      if(settings.fadeInOutTime > 0 && !init) {
            $(wrapperContainer).stop().fadeTo(settings.fadeInOutTime, settings.fadedOutOpacity, function() {
                $(wrapperContainerLink).remove();
                $(wrapperContainer).append(images[i]);
                addPagingEvents(container.substring(1) + "-link");
                $(wrapperContainer).fadeTo(settings.fadeInOutTime, 1, function() {
	            });
		    });
	      } else {
	    	  $(wrapperContainerLink).remove();
	    	  if(init) {
  		        $(wrapperContainer).append(generateLinkImage($(image), $(image).parent()));
	    	  } else {
	    	    $(wrapperContainer).append(images[i]);
	    	  }
	    	  addPagingEvents(container.substring(1) + "-link");
	      }

	      jQuery(wrapperThumbsLinks).each(function(j) {
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

          $(wrapperContainer + "-description").remove();
	      $("<div class='" + container.substring(1) + "-description'><p class='" + container.substring(1) + "-title'>" + $(image).attr("title") + "</p>" + $(image).attr("alt") + "</div>").insertAfter(wrapperContainer);
	      if(($(image).attr("alt") != null && $(image).attr("alt") != "") || ($(image).attr("title") != null && $(image).attr("title") != "")) {
	        $(wrapperContainer + "-description").css("width", $(wrapper + " " + container).width());
          }
	 }

         function addPagingEvents(navClass) {
           $(wrapper + " a." + navClass).click(function(e) {
              //cache active thumb instance
              var $$ = $(wrapperThumbsLinks + ".active");

              if(navClass == "next" || navClass == container.substring(1) + "-link") {
	            if($$.parent().next().length != 0) {
                  $$.parent().next().find("a").click();
                } else {
   	              $(wrapper + " ul li:first a").click();
                }
              } else {
 	            if($$.parent().prev().length != 0) {
                  $$.parent().prev().find("a").click();
	            } else {
                  $(wrapper + " ul li:last a").click();
	            }
	          }
	          e.preventDefault();
           });
           //init
	       if(navClass == "next" || navClass == "prev") {
             fadeMultiple(new Array(wrapper + " a." + navClass,
                                    wrapper + " a." + navClass + " span"), 0, 0);
           }
           $(wrapper + " a." + navClass).hover(function () {
             fadeMultiple(new Array(wrapper + " a.next span",
                                     wrapper + " a.prev span"), settings.fadeNavInOutTime, 0.2);
             if(navClass == "prev") {
	           $(wrapper + " a.prev").stop().fadeTo(settings.fadeNavInOutTime, 1);
	           $(wrapper + " a.next").stop().fadeTo(settings.fadeNavInOutTime, 0.6);
             } else {
               $(wrapper + " a.next").stop().fadeTo(settings.fadeNavInOutTime, 1);
	           $(wrapper + " a.prev").stop().fadeTo(settings.fadeNavInOutTime, 0.6);
             }
	       }, function () {
             fadeMultiple(new Array(wrapper + " a.next", wrapper + " a.next span",
                                    wrapper + " a.prev", wrapper + " a.prev span"), settings.fadeNavInOutTime, 0);
           });

	   if(navClass != "next" && navClass != "prev") {

	     var minHeight = 100;
             var minWidth = 250;

             //cache image instance
             var $$$ = $(wrapperContainerLink + " img");
             
             //IE 6 max-width substitute
             if (jQuery.browser.msie && jQuery.browser.version <= 6) {
               var mainImgHeight = $$$.height();
               if(mainImgHeight > 380) {
                 $$$.css("height", "380px");
               }
             }

	     var imgHeight = $$$.height();
	     var imgWidth = $$$.width();
	     var containerWidth = $(wrapperContainer).width();
	     var imgHeight = (imgHeight < minHeight) ? minHeight : imgHeight;

	     setMultipleCSS(new Array(wrapperContainer + "-nav a", wrapperContainer + "-nav span", 
                                      wrapperContainerLink), "height", imgHeight);
	     if(imgWidth > maxWidth) {
               imgWidth = maxWidth;
	     } else if (imgWidth < minWidth) {
               setMultipleCSS(new Array(wrapperContainerLink), "width", imgWidth);
               imgWidth = minWidth;
	     }
	     setMultipleCSS(new Array(wrapperContainer, wrapperContainer + "-nav"), "width", imgWidth);
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

     function generateLinkImage(theimage, thelink) {
  	   var img = new Image();
  	   var src = $(theimage).attr("src").split("?")[0];
  	   var alt = $(theimage).attr("alt"); img.src = src; img.alt = alt;
  	   link = document.createElement("a");
  	   link.setAttribute("href", $(thelink).attr("href"));
  	   link.setAttribute("class", container.substring(1) + "-link");
  	   link.setAttribute("className", container.substring(1) + "-link"); // IE
  	   return  $(link).append(img);
  	 }

	 function setMultipleCSS(elements, cssProperty, value) {
	   for(var i = 0; i < elements.length; i++) { $(elements[i]).css(cssProperty, value);}
	 }

	 function fadeMultiple(elements, time, opacity) {
	   for(var i = 0; i < elements.length; i++) {$(elements[i]).stop().fadeTo(time, opacity); }
	 }
  };
})(jQuery)