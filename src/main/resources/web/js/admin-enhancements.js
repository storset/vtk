// Used by "createDocumentService" available from "manageCollectionListingService"

function changetemplatename(n) {
  document.createDocumentForm.name.value=n;
}

// Used to hide users and groups when editing permissions

function disableInput() {
  enableDisableInput("none", "5px");
}

function enableInput() {
  enableDisableInput("block", "10px");
}

function enableDisableInput(principalListDisplay, submitButtonsPadding) {
	var p = document.getElementById('principalList');
	if(p) {
	  p.style.display = principalListDisplay;
	}
	var s = document.getElementById('submitButtons');
	if(s) {
	  s.style.paddingTop = submitButtonsPadding;		
	}
}

/*
 * // Used by copyResourceService available from
 * "manageCollectionListingService"
 * 
 * 
 * function copyMoveAction(action) { copyMoveAction(action, 'DEPRECATED: Du må
 * markere minst ett element for flytting eller kopiering'); }
 * 
 * function copyMoveAction(action, unCheckedMessage) { //
 * document.collectionListingForm.target = "_blank";
 * document.collectionListingForm.action = action;
 * 
 * var checked = false;
 * 
 * for (var e = 0; e < document.collectionListingForm.elements.length; e++) { if
 * (document.collectionListingForm.elements[e].type == 'checkbox' &&
 * document.collectionListingForm.elements[e].checked) { checked = true; } }
 * 
 * if (checked) { document.collectionListingForm.submit(); } else {
 * alert(unCheckedMessage); } }
 */

function interceptEnterKey(idOrClass) {
	$("form input" + idOrClass).bind("keypress", function(e) {
            if ((e.which && e.which == 13) || (e.keyCode && e.keyCode == 13)) {
               return false; // cancel the default browser click
           }
      });
}

function interceptEnterKeyAndReroute(txt, btn) {
	$("form " + txt).bind("keypress", function(e) {
            if ((e.which && e.which == 13) || (e.keyCode && e.keyCode == 13)) {
              $(btn).click(); // click the associated button
              return false; // cancel the default browser click
           }
      });
}

function logoutButtonAsLink() {
    var btn = $('input#logoutAction');
    if (btn.size() == 0) {
        return;
    }
    btn.hide();
    btn.after('(&nbsp;<a id=\"logoutAction.link\" name=\"logoutAction\" href="javascript:void(0);">' + btn.attr('value') + '</a>&nbsp;)');
    $('#logoutAction\\.link').click(function() {
        btn.click();
        return false;
    });

}

function copyMoveButtonsAsLinks() {
    var move = $('#vrtx-move-to-selected-folder');
    if (move.size() > 0) {
       var method = move.attr('method');
       var url = move.attr('action');
       var btn = move.children('button');
       btn.hide();
       
       if (method == 'get') {
          btn.after('(&nbsp;<a title="' + btn.attr("title") + '" id="vrtx-move-to-selected-folder.link" href="' + url + '">' + $.trim(btn.text()) + '</a>&nbsp;)');
          btn.addClass('thickbox');
          tb_init('#vrtx-move-to-selected-folder\\.link');
       } else {
          btn.after('(&nbsp;<a title="' + btn.attr("title") + '" id="vrtx-move-to-selected-folder.link" href="javascript:void(0);">' + $.trim(btn.text()) + '</a>&nbsp;)');
          $('#vrtx-move-to-selected-folder\\.link').click(function() {
              btn.click();
              return false;
           });
       }
    }

    var copy = $('#vrtx-copy-to-selected-folder');
    if (copy.size() > 0) {
       var method = copy.attr('method');
       var url = copy.attr('action');
       var btn = copy.children('button');
       var title = $('#vrtx-move-to-selected-folder button').attr("title");
       btn.hide();

       if (method == 'get') {
           btn.after('(&nbsp;<a title="' + btn.attr("title") + '" id="vrtx-copy-to-selected-folder.link" href="' + url + '">' + $.trim(btn.text()) + '</a>&nbsp;)');
           btn.addClass('thickbox');
           tb_init('#vrtx-copy-to-selected-folder\\.link');
       } else {
           btn.after('(&nbsp;<a title="' + btn.attr("title") + '" id="vrtx-copy-to-selected-folder.link" href="javascript:void(0);">' + $.trim(btn.text()) + '</a>&nbsp;)');
           $('#vrtx-copy-to-selected-folder\\.link').click(function() {
               btn.click();
               return false;
           });
       }
    }
}


function placeMoveButtonInActiveTab() {
    var btn = $('#collectionListing\\.action\\.move-resources');
    btn.hide();
    var li = $('li.moveResourcesService');
    li.html('<a id="moveResourceService" href="javascript:void(0);">' + btn.attr('title') + '</a>');
    $('#moveResourceService').click(function() {
        if ($('form[name=collectionListingForm] input[type=checkbox]:checked').size() == 0) {
            alert(moveUncheckedMessage);
        } else {
            $('#collectionListing\\.action\\.move-resources').click();
        }
        return false;
    });
}


function placeCopyButtonInActiveTab() {
    var btn = $('#collectionListing\\.action\\.copy-resources');
    btn.hide();
    var li = $('li.copyResourcesService');
    li.html('<a id="copyResourceService" href="javascript:void(0);">' + btn.attr('title') + '</a>');
    $('#copyResourceService').click(function() {
        if ($('form[name=collectionListingForm] input[type=checkbox]:checked').size() == 0) {
            alert(copyUncheckedMessage);
        } else {
            $('#collectionListing\\.action\\.copy-resources').click();
        }
        return false;
    });
}

function placeDeleteButtonInActiveTab() {
    var btn = $('#collectionListing\\.action\\.delete-resources');
    btn.hide();
    var li = $('li.deleteResourcesService');
    li.html('<a id="deleteResourceService" href="javascript:void(0);">' + btn.attr('title') + '</a>');
    $('#deleteResourceService').click(function() {
        var boxes = $('form[name=collectionListingForm] input[type=checkbox]:checked');
        
        if (boxes.size() == 0) {
            alert(deleteUncheckedMessage);
        } else {
            var list = new String("");
            var boxesSize = boxes.size();
            for(i = 0; i < boxesSize && i < 10;i++){
                list += boxes[i].name + '\n';
            }
            if(boxes.size() > 10){
                list += "... " + confirmDeleteAnd + " " + (boxes.size() - 10) + " " + confirmDeleteMore;
            }
            if(confirm(confirmDelete.replace("(1)",boxes.size()) + '\n\n' +  list)){
                $('#collectionListing\\.action\\.delete-resources').click();
            }
        }
        return false;
    });
}

function placeRecoverButtonInActiveTab() {
	  var btn = $('.recoverResource');
	  if (btn.size() == 0) {
          return;
      }
      btn.hide();
      $("#main .activeTab").prepend('<ul class="listMenu tabMenu2"><li class="recoverResourceService"><a id="recoverResourceService" href="javascript:void(0);">' + btn.attr('value') + '</a></li></ul>');
      $('#recoverResourceService').click(function() {
    	var boxes = $('form.trashcan input[type=checkbox]:checked');
    	//TODO i18n from somewhere
        var recoverUncheckedMessage = 'You must check at least one element to recover';
    	
    	if (boxes.size() == 0) {
            alert(recoverUncheckedMessage); //TODO i18n from somewhere
        } else {
        	$('.recoverResource').click();
        }
    	  
    	return false;  
      });
}

function placeDeletePermanentButtonInActiveTab() {
      var btn = $('.deleteResourcePermanent');
      if (btn.size() == 0) {
          return;
      }
      btn.hide();
      $("#main .activeTab .tabMenu2").append('<li class="deleteResourcePermanentService"><a id="deleteResourcePermanentService" href="javascript:void(0);">' + btn.attr('value') + '</a></li>');
      $('#deleteResourcePermanentService').click(function() {
        var boxes = $('form.trashcan input[type=checkbox]:checked');
        //TODO i18n from somewhere
        var deletePermanentlyUncheckedMessage = 'You must check at least one element to delete permanently'; 
        
        if (boxes.size() == 0) {
        	alert(deletePermanentlyUncheckedMessage);
        } else {     
        	//TODO i18n from somewhere
        	var confirmDeletePermanently = 'Are you sure you want to delete:';   
        	var confirmDeletePermanentlyAnd = 'and';
        	var confirmDeletePermanentlyMore = 'more';
        	
            var list = new String("");
            var boxesSize = boxes.size();
            for(i = 0; i < boxesSize && i < 10;i++){
                list += boxes[i].title + '\n';
            }
            if(boxes.size() > 10){
                list += "... " + confirmDeletePermanentlyAnd + " " + (boxes.size() - 10) + " " + confirmDeletePermanentlyMore;
            }
            if(confirm(confirmDeletePermanently.replace("(1)",boxes.size()) + '\n\n' +  list)){
                $('.deleteResourcePermanent').click();
            }
        }
        return false;
     });
}

function unlockButtonAsLink() {
    var btn = $('#vrtx-unlock-resource-form\\.submit');
    if (btn.size() == 0) {
        return;
    }
    btn.hide();
    btn.after('(&nbsp;<a id="vrtx-unlock-resource-form.link" href="javascript:void(0);">' + btn.attr('value') + '</a>&nbsp;)');
    $('#vrtx-unlock-resource-form\\.link').click(function() {
        btn.click();
        return false;
    });
}

function toggleAclInheritanceButtonAsLink() {
    var btn = $('#permissions\\.toggleInheritance\\.submit');
    if (btn.size() == 0) {
        return;
    }
    btn.hide();
    btn.after('(&nbsp;<a id="permissions.toggleInheritance.link" href="javascript:void(0);">' + btn.attr('value') + '</a>&nbsp;)');
    $('#permissions\\.toggleInheritance\\.link').click(function() {
        btn.click();
        return false;
    });
}

function takeOwnershipButtonAsLink() {
    var btn = $('#vrtx-admin-ownership-button');
    if (btn.size() == 0) {
        return;
    }
    btn.hide();
    btn.after('(&nbsp;<a id="vrtx-admin-ownership-link" href="javascript:void(0);">' + $.trim(btn.text()) + '</a>&nbsp;)');
    $('#vrtx-admin-ownership-link').click(function() {
        btn.click();
        return false;
    });
}

function leftAdjustTabMessagePublishPermissionIfRootFolder() {
	if($("li.navigateToParentService").length <= 0) {
	   	$("#vrtx-manage-collectionlisting div.tabMessagePublishPermission span").css("marginLeft", "0px");
	}
}

function checkAll(){
    $(".checkbox input").each(function(){
        this.checked = true;
        switchCheckedClasslass(this);
    });
}

function uncheckAll(){
    $(".checkbox input").each(function(){
        this.checked = false;
        switchCheckedClasslass(this);
    });
}

function checkedClass(){
    if(this.checked){
        this.checked = false;
    }else{
        this.checked = true;
    }
    switchCheckedClasslass(this);
}

function switchCheckedClasslass(obj){
    if(obj.checked){
        $(obj).parent().parent().addClass("checked");
    }else{
        $(obj).parent().parent().removeClass("checked");
    }
}

// Add callbacks for the above methods:

$(document).ready(logoutButtonAsLink);
$(document).ready(copyMoveButtonsAsLinks);
$(document).ready(placeMoveButtonInActiveTab);
$(document).ready(placeCopyButtonInActiveTab);
$(document).ready(placeDeleteButtonInActiveTab);
$(document).ready(placeRecoverButtonInActiveTab);
$(document).ready(placeDeletePermanentButtonInActiveTab);
$(document).ready(unlockButtonAsLink);
$(document).ready(toggleAclInheritanceButtonAsLink);
$(document).ready(takeOwnershipButtonAsLink);
$(document).ready(leftAdjustTabMessagePublishPermissionIfRootFolder);

$(document).ready(function(){    
    $(".vrtx-check-all").click(checkAll);
    $(".vrtx-uncheck-all").click(uncheckAll);
    $(".checkbox input").click(checkedClass);
    $(".checkbox").click(
        function(){
            $(this).find("input").each(checkedClass);
    });
});

$(document).ready(function() {
    //TODO: AJAX get JSON of resources
	
	// Dummy JSON
	var approve = [
        {
          "title": "artikkel.html",
          "uri": "/artikkel.html",
          "approved" : false
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : false
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : false
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : false
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : false
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : false
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : false
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : false
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : false
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : true
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : false
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : false
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : false
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : false
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : false
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : false
        },
        {
          "title": "artikkel2.html",
          "uri": "/om/artikkel.html",
          "approved" : false
        }
      ];
	//TODO: i18n
    $("#resource\\.manually-approve-from").focus(function() {
       var pages = 1, prPage = 10, len = approve.length;
       var total = len > prPage ? (parseInt(len / 10) + 1) : 1; 
       
       var html = "<div id='approve-page-" + pages + "'><h3>Manually approve resources - page " + pages + "/" + total + "</h3><ul>";
       for(var i = 0; i < len; i++) {
         if(approve[i].approved) {
           html += "<li><input type='checkbox' checked='checked' />";
         } else {
           html += "<li><input type='checkbox' />";
         }
         html += "<a href='" + approve[i].uri + "'>" + approve[i].title + "</a></li>";
         if((i+1) % prPage == 0) {
           pages++;
           html += "</ul>";
           if(i > prPage && i < len-1) {
             html += "<a href='#page-" + (pages - 1) + "' class='prev' id='page-" + (pages - 1) + "'>Prev</a>";
           }
           if(i < len-1) {
             html += "<a href='#page-" + pages + "' class='next' id='page-" + pages + "'>Next</a>";
             html += "</div><div id='approve-page-" + pages + "'>";
             html += "<h3>Manually approve resources - page " + pages + "/" + total + "</h3>";
           }
           html += "<ul>";
         }
       }
       html += "</ul><a href='#page-" + (pages - 1) + "' class='prev' id='page-" + (pages - 1) + "'>Prev</a></div>";
       html += "<div id='manually-approve-save-cancel'><input type='submit' id='manually-approve-save' value='Save' />"
    	     + "<a href='#' id='manually-approve-cancel'>Cancel</a></div>";
       $("#manually-approve-container").html(html).slideDown("fast");
       $("#manually-approve-container div").not("#approve-page-1").not("#manually-approve-save-cancel").hide();
    });
    
    $("#manually-approve-container").delegate("#manually-approve-save", "click", function() {
    	// TODO: implement save..
    	
    	$(this).parent().parent().slideUp("fast");
        return false;  
    });
    
    $("#manually-approve-container").delegate("#manually-approve-cancel", "click", function() {
    	$(this).parent().parent().slideUp("fast");
        return false;  
    });

    // Paging - next
    $("#manually-approve-container").delegate(".next", "click", function() {
      var that = $(this).parent();
      var next = that.next();
      if(next) {
         $(that).hide();    
         $(next).show();
      }
      return false;  
    });
    // Paging - previous
    $("#manually-approve-container").delegate(".prev", "click", function() {
      var that = $(this).parent();
      var prev = that.prev();
      if(prev) {
         $(that).hide();    
         $(prev).show();
      }
      return false;  
    });
 });