<#ftl strip_whitespace=true>
<#--
  - File: 
  - 
  - Required model data:
  -  
  -  
  - Optional model data:
  -
  -->
<#import "/lib/ping.ftl" as ping />
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/fckeditor/fckeditor-textarea.ftl" as fck />
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
  <head>
    <title>Editor</title>
    <@fck.declareEditor />
    <@ping.ping url=pingURL['url'] interval=300 />

    <#assign doc = resource.content />
    <#assign body = (doc.selectSingleElement("html.body"))?default('') />
    <#assign userTitle = resource.getValueByName('userTitle')?default('') />
    <#assign docTitle = (doc.selectSingleElement("html.head.title").content)?default('') />
    <#if userTitle == '' && docTitle != ''>
      <#assign userTitle = docTitle />
    </#if>
    <#assign h1 = (doc.selectSingleElement("html.body.h1(0)"))?default('') />
    <#if h1 != '' && h1.content == userTitle>
      ${body.removeContent(h1)}
    </#if>
  </head>
  <body>
    <form method="POST" action="#submit">

      <#-- Preserve properties not edited here: -->
      <#list resource.contentProperties + resource.extraContentProperties as propDef>
        <#if propDef.name != 'userTitle'>
          <input type="hidden" name="resource.${propDef.name}"  value="${resource.getValue(propDef)?html}" /> 
        </#if>
      </#list>

      <div class="properties">
        <div class="htmlTitle property-item">
          <label for="title"><@vrtx.msg code="fck.documentTitle" default="Document title" /></label> 
          <input type="text" name="resource.userTitle" size="40" value="${userTitle}" />
        </div>
      </div>

      <div class="html-content">
        <label for="resource.content"><@vrtx.msg code="editor.content" /></label> 
        <textarea name="resource.content" rows="8" cols="60"
                id="resource.content">${doc.stringRepresentation?html}</textarea>

        <@fck.editorInTextarea textarea='resource.content'
                               fontFormats='p;h2;h3;h4;h5;h6;pre'
                               fullpage=true enableFileBrowsers=true />
      </div>

      <div id="submit" class="save-cancel">
       <input type="submit" name="save" value="${vrtx.getMsg("editor.save")}">
       <input type="submit" name="savequit" value="${vrtx.getMsg("editor.saveAndQuit")}">
       <input type="submit" name="cancel" value="${vrtx.getMsg("editor.cancel")}">
      </div>


    </form>
  </body>
</html>
