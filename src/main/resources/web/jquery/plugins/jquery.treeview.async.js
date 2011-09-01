/*
 * Async Treeview 0.1 - Lazy-loading extension for Treeview
 * 
 * http://bassistance.de/jquery-plugins/jquery-plugin-treeview/
 *
 * Copyright (c) 2007 JÃ¶rn Zaefferer
 *
 * Dual licensed under the MIT and GPL licenses:
 *   http://www.opensource.org/licenses/mit-license.php
 *   http://www.gnu.org/licenses/gpl.html
 *
 * Revision: $Id$
 *
 * USIT added JSON: 1. possible to set classes also on <li> (in addition to <span>)
 *                  2. uri in <a>
 *                  3. title in <a>
 *                  4. update settings.url on toggle()
 *                  5. service
 *
 * USIT * removed commented out method and set indent to 2 spaces
 *      * added error debug to AJAX function
 *
 */

;
(function ($) {

  function load(settings, root, child, container) {
    function createNode(parent) {
      var linkOrPlainText = "";
      if (this.uri) {
        if (this.title) {
          var theuri = this.uri;
          linkOrPlainText = "<a class='tree-link' href='" + theuri + "' title='" + this.title + "'>" + this.text + "</a>"
        } else {
          linkOrPlainText = "<a href='" + this.uri + "'>" + this.text + "</a>"
        }
      } else {
        linkOrPlainText = this.text;
      }
      var current = $("<li/>").attr("id", this.id || "").html("<span>" + linkOrPlainText + "</span>").appendTo(parent);

      if (this.listClasses) current.addClass(this.listClasses);
      if (this.spanClasses) current.children("span").addClass(this.spanClasses);
      if (this.expanded) current.addClass("open");

      if (this.hasChildren || this.children && this.children.length) {
        var branch = $("<ul/>").appendTo(current);
        if (this.hasChildren) {
          current.addClass("hasChildren");
          createNode.call({
            classes: "placeholder",
            text: "&nbsp;",
            children: []
          }, branch);
        }
        if (this.children && this.children.length) {
          $.each(this.children, createNode, [branch])
        }
      }
    }
    $.ajax($.extend(true, {
      url: settings.url,
      dataType: "json",
      data: {
        root: root
      },
      success: function (response) {
        child.empty();
        $.each(response, createNode, [child]);
        $(container).treeview({
          add: child
        });
      },
      error: function (jqXHR, textStatus, errorThrown) {
        if (typeof console !== "undefined" && console.log) {
          console.log(textStatus);
          if (console.dir) {
            console.dir(jqXHR);
          }
        }
      }
    }, settings.ajax));
  }

  var proxied = $.fn.treeview;
  $.fn.treeview = function (settings) {
    if (!settings.url) {
      return proxied.apply(this, arguments);
    }
    var container = this;
    if (!container.children().size()) load(settings, "source", this, container);
    var userToggle = settings.toggle;
    return proxied.call(this, $.extend({}, settings, {
      collapsed: true,
      toggle: function () {
        var $this = $(this);
        if ($this.hasClass("hasChildren")) {
          var subFolder = $(this).find("a").attr("href").split("?")[0];
          if ($.browser.msie && $.browser.version <= 7) { // dirty but only way to fix broken IE 7 DOM
            var locationHref = window.location.href;
            var uioCutpoint = "uio.no/";
            var devCutpoint = ":9322/";
            var uioEnd = locationHref.indexOf(uioCutpoint) + uioCutpoint.length;
            uioEnd = uioEnd > uioCutpoint.length ? uioEnd : locationHref.indexOf(devCutpoint) + devCutpoint.length;
            var base = locationHref.substring(0, uioEnd);
            subFolder = "/" + subFolder.replace(base, "");
          }
          var timestamp = 1 - new Date();
          var ajaxUrl = {
            url: "?vrtx=admin&service=" + settings.service + "&uri=" + subFolder + "&ts=" + timestamp
          };
          $.extend(settings, ajaxUrl);
          var childList = $this.removeClass("hasChildren").find("ul");
          load(settings, this.id, childList, container);
        }
        if (userToggle) {
          userToggle.apply(this, arguments);
        }
      }
    }));
  };

})(jQuery);