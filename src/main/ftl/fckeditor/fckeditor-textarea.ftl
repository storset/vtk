<#--
 * editorInTextArea
 *
 * Display a minimal FCKeditor in a div.
 *
 * @param textarea - the id of the textarea to replace with FCKeditor
 * @param fckeditorBase - the FCKeditor config (required to contain a 'url' entry)
 * @param runOnLoad - whether to run the editor immediately or wait
 *        until the JavaScript function loadEditor() is
 *        invoked. Defaults to 'false'.
 * @validElements - a list of [name, attribute-list] maps that
 *        describe the valid HTML elements
 * @toolbarElements - a list of strings that describe the set of
 *        editor toolbar elements to use
 *
-->

<#macro editorInTextarea fckeditorBase textarea  runOnLoad=true
          validElements=[{"name":"b", "attributes":[]},
                         {"name":"strong", "attributes":[]},
                         {"name":"i", "attributes":[]},
                         {"name":"em", "attributes":[]},
                         {"name":"a", "attributes":["href"]},
                         {"name":"ul", "attributes":[]},
                         {"name":"ol", "attributes":[]},
                         {"name":"li", "attributes":[]},
                         {"name": "p", "attributes":[]}]
          toolbarElements=["Source", "Bold", "Italic", "Underline",
                           "StrikeThrough", "OrderedList",
                           "UnorderedList", "Link", "Unlink"]>
    <script type="text/javascript" src="${fckeditorBase.url?html}/fckeditor.js"></script>
    <script type="text/javascript">
      var initialized = false;

      <#-- XXX: this whitelist is currently unused, filtering is done server-side: -->
      var whitelist_elements = [
        <#compress>
        <#list validElements as element>
          ["${element.name?html}"
          <#if (element.attributes)?exists && (element.attributes)?size &gt; 0>
            [<#list element.attributes as attr>
              "${attr?html}"<#if attr_has_next>, </#if>
            </#list>]
          </#if>
          ]<#if element_has_next>, </#if>
        </#list>];
        </#compress>


      function loadEditor() {
          if (initialized) return;
          var editor = new FCKeditor("${textarea}");
          editor.BasePath = "${fckeditorBase.url?html}/";
          editor.Config['ToolbarSets'] = "( {'MinimalToolbar' : [\
                 [<#list toolbarElements as elem>'${elem}'<#if elem_has_next>,</#if></#list>]\
	      ]} )";
          editor.ToolbarSet = "MinimalToolbar";
          editor.Config['FullPage'] = false;
          editor.Config['ToolbarCanCollapse'] = false;
          editor.ReplaceTextarea();
          initialized = true;
      }
      <#if runOnLoad>
        loadEditor();
      </#if>
    </script>
</#macro>
