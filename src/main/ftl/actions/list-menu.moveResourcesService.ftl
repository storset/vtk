<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#assign unCheckedMessage = vrtx.getMsg("tabMenu2.moveUnCheckedMessage",
         "You must check at least one element to move") />

<script type="text/javascript" language="Javascript"><!--

 function checked() {
    for (var e = 0; e < document.collectionListingForm.elements.length; e++) {
       if (document.collectionListingForm.elements[e].type == 'checkbox' && document.collectionListingForm.elements[e].checked) {
           return true;
       }
    }
    return false;
 }

 function move() {
    if (!checked()) {
        alert('${unCheckedMessage}');
    } else {
        document.getElementById('collectionListing.action.move-resources').click();
    }
 }
 document.write('${prepend}');
 document.write('<a href="javascript:move();">${item.title}</a>');
 document.write('${append}');
 //-->
</script>
<#recover>
${.error}
</#recover>
