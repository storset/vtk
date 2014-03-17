/*
 * VrtxTree - facade to TreeView async
 *  
 *  * Requires Dejavu OOP library
 *  * Requires but Lazy-loads TreeView and ScrollTo libraries (if not defined) on open
 */

var VrtxTreeInterface = dejavu.Interface.declare({
  $name: "VrtxTreeInterface",
  __opts: {},
  __openLeaf: function() {}
});

var VrtxTree = dejavu.Class.declare({
  $name: "VrtxTree",
  $implements: [VrtxTreeInterface],
  $constants: {
    leafLoadingClass: "loading-tree-node",
    leafSelector: "> .hitarea" // From closest li
  },
  __opts: {},
  initialize: function(opts) {
    var tree = this;
    tree.__opts = opts;
    tree.__opts.pathNum = 0;
    
    // TODO: rootUrl and jQueryUiVersion should be retrieved from Vortex config/properties somehow
    var rootUrl = "/vrtx/__vrtx/static-resources";
    var jQueryUiVersion = "1.10.4";
    
    $.loadCSS(location.protocol + "//" + location.host + rootUrl + "/themes/default/report/jquery.treeview.css");
    var futureTree = $.Deferred();
    if (typeof $.fn.treeview !== "function") {
      $.getScript(location.protocol + "//" + location.host + rootUrl + "/jquery/plugins/jquery.treeview.js", function () {
        $.getScript(location.protocol + "//" + location.host + rootUrl + "/jquery/plugins/jquery.treeview.async.js", function () {
          futureTree.resolve();
        });
      });
    } else {
      futureTree.resolve();
    }
    var futureScrollTo = $.Deferred();
    if(typeof $.fn.scrollTo !== "function" && tree.__opts.scrollToContent) {
      $.getScript(location.protocol + "//" + location.host + rootUrl + "/jquery/plugins/jquery.scrollTo.min.js", function () {
        futureScrollTo.resolve();
      });
    } else {
      futureScrollTo.resolve();
    }
    $.when(futureTree, futureScrollTo).done(function() {
      opts.elem.treeview({
        animated: "fast",
        url: location.protocol + '//' + location.host + location.pathname + "?vrtx=admin&uri=&" + opts.service + "&ts=" + (+new Date()),
        service: opts.service,
        dataLoaded: function () {
          tree.__openLeaf();
        }
      });
    });
  },
  __openLeaf: function() {
    var tree = this;
    var checkLeafAvailable = setInterval(function () {
      $("." + tree.$static.leafLoadingClass).remove();
      var link = tree.__opts.elem.find("a[href$='" + tree.__opts.trav[tree.__opts.pathNum] + "']");
      if (link.length) {
        clearInterval(checkLeafAvailable);
        var hit = link.closest("li").find(tree.$static.leafSelector);
        hit.click();
        if (tree.__opts.scrollToContent && (tree.__opts.pathNum == (tree.__opts.trav.length - 1))) {
          tree.__opts.elem.css("background", "none").fadeIn(200, function () {  // Scroll to node
            $(tree.__opts.scrollToContent).scrollTo(Math.max(0, (link.position().top - 145)), 250, {
              easing: "swing",
              queue: true,
              axis: 'y',
              complete: tree.__opts.afterTrav(link)
            });
          });
        } else {
          if (tree.__opts.scrollToContent) { // Follow scroll
            $(tree.__opts.scrollToContent).scrollTo(Math.max(0, (link.position().top - 145)), 20, {
              easing: "swing",
              queue: true,
              axis: 'y'
            });
          }
          $("<span class='" + tree.$static.leafLoadingClass + "'>" + loadingSubfolders + "</span>").insertAfter(hit.next());
        }
        tree.__opts.pathNum++;
      }
    }, 20);
  }
});