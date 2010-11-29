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
	  
	  //register arrow left and right keys as 'prev' and 'next'
	  $(document).keydown(function(e){
	    if (e.keyCode == 37) { 
	      $(wrapper + " a.prev").click(); 
	    } else if (e.keyCode == 39) {
	      $(wrapper + " a.next").click();
	    }
	  });

          //Performance: function pointers to use inside loop
          var centerThumbnailImageFunc = centerThumbnailImage;
          var calculateImageFunc = calculateImage;
          var generateLinkImageFunc = generateLinkImage;

	  //init first active image
	  calculateImageFunc($(wrapperThumbsLinks + ".active img"), 0, true);

          //TODO: use for-loop and optimize use of sub-functions
	  return this.each(function (i) {

               var link = generateLinkImageFunc($("img", this), $(this));
	       images[i] = link; //cache image

               $(this).hover(function () { if(!$(this).hasClass("active")) { $("img", this).stop().fadeTo(settings.fadeThumbsInOutTime, 1); } },
                             function () { if(!$(this).hasClass("active")) { $("img", this).stop().fadeTo(settings.fadeThumbsInOutTime, settings.fadedThumbsOutOpacity); }
               });

               $(this).click(function(e) {
	               calculateImageFunc($("img", this), i, false);
	  	       $(this).addClass("active");
	  	       $("img", this).stop().fadeTo(0, 1);
	               e.preventDefault();
               });

               centerThumbnailImageFunc($("img", this));
	 });

	 function calculateImage(image, i, init) {
	      if(settings.fadeInOutTime > 0 && !init) {
                $(wrapperContainer).stop().fadeTo(settings.fadeInOutTime, settings.fadedOutOpacity, function() {
                  $(wrapperContainerLink).remove();
                  $(wrapperContainer).append(images[i]);
                  $(wrapperContainer).fadeTo(settings.fadeInOutTime, 1, function() {}, "easeOutQuad");
                  $(wrapperContainer + " img").fadeTo(0, 1)
                  addPagingEvents(container.substring(1) + "-link");
		}, "easeInQuad");
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
	           $(wrapper + " a.next").stop().fadeTo(settings.fadeNavInOutTime, 0.5);
             } else {
               $(wrapper + " a.next").stop().fadeTo(settings.fadeNavInOutTime, 1);
	           $(wrapper + " a.prev").stop().fadeTo(settings.fadeNavInOutTime, 0.5);
             }
	   }, function () {
             fadeMultiple(new Array(wrapper + " a.next", wrapper + " a.next span",
                                    wrapper + " a.prev", wrapper + " a.prev span"), settings.fadeNavInOutTime, 0);
           });

	   if(navClass != "next" && navClass != "prev") {
             scaleAndCalculatePosition();
	   }
     }
         
     function scaleAndCalculatePosition() {

         var minHeight = 100;
         var minWidth = 150;

         //cache image instance
         var $$$ = $(wrapperContainerLink + " img");
         var imgHeight = $$$.height();
	 var imgWidth = $$$.width();
	     
         //IE 6 max-height substitute
         if (jQuery.browser.msie && jQuery.browser.version <= 6) {
           if(imgHeight > 380) {
             $$$.css("height", "380px");
             imgHeight = 380;
           }
         }
     
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
  	   return $(link).append(img);
  	 }

	 function setMultipleCSS(elements, cssProperty, value) {
           var elementsLength = elements.length;
	   for(var i = 0; i < elementsLength; i++) { $(elements[i]).css(cssProperty, value);}
	 }

	 function fadeMultiple(elements, time, opacity) {
           var elementsLength = elements.length;
	   for(var i = 0; i < elementsLength; i++) {$(elements[i]).stop().fadeTo(time, opacity); }
	 }
  };
})(jQuery)

// Used easing algorithms follows below. 
// TODO: Probably include the whole library (it is very small) in Vortex instead later.

/*
 * jQuery Easing v1.3 - http://gsgd.co.uk/sandbox/jquery/easing/
 *
 * Uses the built in easing capabilities added In jQuery 1.1
 * to offer multiple easing options
 *
 * TERMS OF USE - jQuery Easing
 * 
 * Open source under the BSD License. 
 * 
 * Copyright © 2008 George McGinley Smith
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, 
 * are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of 
 * conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list 
 * of conditions and the following disclaimer in the documentation and/or other materials 
 * provided with the distribution.
 * 
 * Neither the name of the author nor the names of contributors may be used to endorse 
 * or promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *  COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 *  GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED 
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED 
 * OF THE POSSIBILITY OF SUCH DAMAGE. 
 *
*/

jQuery.easing['jswing'] = jQuery.easing['swing'];

jQuery.extend( jQuery.easing,
{
	def: 'easeOutQuad',
	swing: function (x, t, b, c, d) {
		//alert(jQuery.easing.default);
		return jQuery.easing[jQuery.easing.def](x, t, b, c, d);
	},
	easeInQuad: function (x, t, b, c, d) {
		return c*(t/=d)*t + b;
	},
	easeOutQuad: function (x, t, b, c, d) {
		return -c *(t/=d)*(t-2) + b;
	}
});
