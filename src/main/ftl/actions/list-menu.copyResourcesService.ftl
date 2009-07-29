<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#assign unCheckedMessage = vrtx.getMsg("tabMenu2.copyUnCheckedMessage",
         "You must check at least one element to copy") />

<script type="text/javascript" language="Javascript"><!--
 function checked() {
    for (var e = 0; e < document.collectionListingForm.elements.length; e++) {
       if (document.collectionListingForm.elements[e].type == 'checkbox' && document.collectionListingForm.elements[e].checked) {
          return true;
       }
    }
    return false;
 }

 function copy() {
    if (!checked()) {
        alert('${unCheckedMessage}');
    } else {
        document.getElementById('collectionListing.action.copy-resources').click();
    }
 }

 document.write('${prepend}');
 document.write('<a href="javascript:copy();">${item.title}</a>');
 document.write('${append}');

 //-->
</script>
<#recover>
${.error}
</#recover>

