// Used by articles and events

function deleteCommentButtonsAsLink() {
	$(".comment-delete-button").each(function(i){
      var btn = $(this);
      if (btn.size() != 0) {
        btn.hide();
        btn.after('(&nbsp;<a class="comment-delete-link" id="comment-delete-link-' + i + '" href="javascript:void(0);">' + $.trim(btn.text()) + '</a>&nbsp;)');
        $("#comment-delete-link-" + i).click(function() {
            btn.click();
            return false;
        });
      } 
	});
}

function deleteAllCommentsButtonAsLink() {
	var btn = $('#vrtx-comments-delete-all');
    if (btn.size() == 0) {
        return;
    }
    btn.hide();
    btn.after('(&nbsp;<a id="vrtx-comments-delete-all-link" href="javascript:void(0);">' + $.trim(btn.text()) + '</a>&nbsp;)');
    $('#vrtx-comments-delete-all-link').click(function() {
        btn.click();
        return false;
    });
}

// Add callbacks for the above methods:
$(document).ready(deleteAllCommentsButtonAsLink);
$(document).ready(deleteCommentButtonsAsLink);