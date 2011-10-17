<#ftl strip_whitespace=true>
<#--
  - File: comments-component.ftl
  - 
  - Description: displays a list of comments on the page.
  - 
  - Required model data:
  -   comments
  -   commentsEnabled
  -   config
  -
  - Optional model data:
  -   form
  -   principal
  - 
  -->
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/ckeditor/ckeditor-textarea.ftl" as ck />

<#if !comments?exists || !config?exists>
  <#stop "Unable to render model: model data missing: required entries
          are 'comments' and 'config'">
</#if>

<#if commentsEnabled>
  <@ck.declareEditor />
</#if>

<div class="vrtx-comments" id="comments">
  <#if comments?exists>
    <div class="comments-header">

    <div id="comments-header-left">
      <#assign header>
        <@vrtx.msg code="commenting.header"
                   default="Comments (" + comments?size + ")"
                   args=[comments?size] />
      </#assign>
      <#if baseCommentURL?exists>
        <a class="header-href" href="${(baseCommentURL + '#comments')?html}">${header}</a>
      <#else>
        ${header}
      </#if>
  
        <#assign message><@vrtx.msg code="commenting.deleteall" default="delete all" /></#assign>
        <#assign confirmation>
          <@vrtx.msg code="commenting.deleteall.confirmation" 
                     default="Are you sure you want to delete all ${comments?size} comments?" 
                     args=[comments?size] />
        </#assign>  
  
      <#if deleteAllCommentsURL?exists>
        <form class="vrtx-comments-delete-all" action="${deleteAllCommentsURL?html}" method="post">
          <@vrtx.csrfPreventionToken url=deleteAllCommentsURL />
          <button type="submit" id="vrtx-comments-delete-all" name="delete-all-comments-button"
                  onclick="return confirm('${confirmation}');">
            ${message?html}
          </button>
        </form>
      </#if>
    </div>
    
    <#if feedURL?exists>
      <div id="comments-header-feedHref">
         <a href="${feedURL?html}"><@vrtx.msg code="commenting.subscribe" default="subscribe" /></a>
      </div>
    </#if>
  </#if>
 </div>

  <#assign comments = comments?sort_by("time") />

  <#assign rowclass="even" />

    <#list comments as comment>
      <div class="vrtx-comment ${rowclass}" id="comment-${comment.ID?html}">
        <#if config.titlesEnabled>
          <div class="comment-title">
            <#if baseCommentURL?exists>
              <a href="${(baseCommentURL + '#comment-' + comment.ID)?html}">${comment.title?html}</a>
              <#else>
                ${comment.title?html}
              </#if>
          </div>
        </#if>
      <div class="comment-body">
      <#if config.htmlContentEnabled>
        <#-- Display content as raw html: -->
        ${comment.content}
      <#else>
        <#-- Display content as escaped html: -->
        ${comment.content?html}
      </#if>
      </div>
      <div class="comment-info">
        <#if comment.author.URL?exists>
         <span class="comment-author"><a href="${comment.author.URL?html}">${comment.author.description?html}</a></span><span class="comment-author-line"> -</span>
        <#else>
         <span class="comment-author"><@vrtx.breakSpecificChar nchars=21 char='@'>${comment.author.description?html}</@vrtx.breakSpecificChar></span><span class="comment-author-line"> -</span>
        </#if>
         <span class="comment-date"><@vrtx.date value=comment.time format='long' /></span>
        <#if deleteCommentURLs[comment.ID?html]?exists>
          <#assign message><@vrtx.msg code="commenting.delete" default="delete" /></#assign>
          <#assign confirmation>
            <@vrtx.msg code="commenting.delete.confirmation" 
                       default="Are you sure you want to delete this comment?" />
          </#assign>
          <form class="vrtx-comments-delete" action="${deleteCommentURLs[comment.ID]?html}" method="post">
            <@vrtx.csrfPreventionToken url=deleteCommentURLs[comment.ID] />
            <button class="comment-delete-button" type="submit" onclick="return confirm('${confirmation}');">${message?html}</button>
          </form>
        </#if>
      </div>
      <span class="vrtx-comment-background-hook-1"></span>
    </div>
    <#assign rowclass><#if rowclass="even">odd<#else>even</#if></#assign>
  </#list>
  
  <div class="add-comment" id="comment-form">
    <div class="add-comment-header"><@vrtx.msg code="commenting.form.add-comment" default="Add comment" /></div>
    <#if !commentsEnabled>
       <p><@vrtx.msg code="commenting.disabled"
                  default="Commenting is disabled on this resource." /></p>
    <#elseif !principal?exists>
      <#assign completeLoginURL>${loginURL?html}&amp;anchor=comment-form</#assign>
      <#assign defaultMsg>
        To comment on this resource you have to <a href="${completeLoginURL}">log in</a>
      </#assign>
      <p><@vrtx.rawMsg code="commenting.not-logged-in" default=defaultMsg args=[completeLoginURL] /></p>

    <#elseif repositoryReadOnly>
       <p><@vrtx.msg code="commenting.read.only"
                    default="You cannot add comments because the system is currently in read only mode." /></p>
      
    <#elseif commentsAllowed && commentsLocked>
      <p><@vrtx.msg code="commenting.locked"
                    default="You can not comment this resource at the moment because it is locked by another user." /></p>

    <#elseif !commentsAllowed>
      <p><@vrtx.msg code="commenting.denied"
                    default="You are not allowed to comment on this resource." /></p>

    <#elseif commentsAllowed && postCommentURL?exists>
      <div id="comment-syntax-desc" class="comment-syntax-desc">
        <div class="syntax-head"><@vrtx.msg code="commenting.form.syntax-description" default="Allowed HTML syntax" />:</div>
        <p>
        <#list config.validHtmlElements as element>
          &lt;${element.name?html}<#compress>
          <#if (element.attributes)?exists>
            <#list element.attributes as attr>
              &nbsp;${attr?html}=".."
            </#list>
          </#if>
          </#compress>&gt;<#if element_has_next>, </#if>
        </#list>
        </p>
      </div>
      <form class="vrtx-comments-post" action="${postCommentURL?string?html}#comment-form" method="post">
        <@vrtx.csrfPreventionToken url=postCommentURL />
        <#if config.titlesEnabled>
        <div class="comments-title">
          <#assign value><#if form?exists && form.title?exists>${form.title}</#if></#assign>
          <label for="comments-title">
            <@vrtx.msg code="commenting.form.title" default="Title" /></label>
          <input id="comments-title" type="text" name="title" size="30" value="${value}" />
          <#if errors?exists && errors.getFieldError('title')?exists>
            <span class="error">
              <@vrtx.msg code=errors.getFieldError('title').getCode()
                         default=errors.getFieldError('title').getDefaultMessage() />
            </span>
          </#if>
        </div>
        </#if>
        <div class="comments-text" id="comments-text-div">
          <#assign value><#if form?exists && form.text?exists>${form.text}</#if></#assign>
          <textarea id="comments-text" name="text" rows="6" cols="80">${value?html}</textarea>
        <#if errors?exists && errors.getFieldError('text')?exists>
          <div class="error">
            <@vrtx.msg code=errors.getFieldError('text').getCode()
                       default=errors.getFieldError('text').getDefaultMessage() 
                       args=errors.getFieldError('text').getArguments()  />
          </div>
        </#if>
        </div>
        <div class="submit">
        <#assign principalStr>
          <#compress>
            <span class="user">${principal.description?html}</span>
          </#compress>
        </#assign>
        <input type="submit" id="submit-comment-button" name="save"
          value="<@vrtx.msg code='commenting.form.submit'
          default='Submit' />" />
          (<@vrtx.rawMsg code="commenting.post.comment-as" default="as ${principal.description}" args=[principalStr] />)
        </div> 
      </form>
      <@ck.editorInTextarea textarea="comments-text" toolbar="AddComment" runOnLoad=false  />
    </#if>
  </div>
</div>
