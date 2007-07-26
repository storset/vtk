<#import "/lib/vortikal.ftl" as vrtx />

<style type="text/css">

  div.comments-header {
     margin-top: 1em;
     margin-bottom: 2em;
  }
  div.comments-header a.header-href {
     font-size: 110%;
     font-weight: bold;
  }
  div.vrtx-comment {
     margin-bottom: 2em;
  }
  div.comment-info {
     font-size: 90%;
     padding-top: 1em;
  }
  .even {
  }
  .odd {
  }
  div.add-comment-header {
     font-size: 110%;
     font-weight: bold;
  }
</style>


<div class="vrtx-comments" id="comments">

  <#if comments?exists>
    <hr />
    <div class="comments-header">
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
    <#if comments?size &gt; 1>
      <#if deleteAllCommentsURL?exists>(&nbsp;<a onclick="return confirm('Are you sure?')" href="${deleteAllCommentsURL?html}">delete all</a>&nbsp;)</#if>
    </#if>
   </div>

    <#assign comments = comments?sort_by("time") />

    <#assign rowclass="even" />

    <#list comments as comment>
      <div class="vrtx-comment ${rowclass}" id="comment-${comment.ID?c}">
        <#if config.titlesEnabled>
          <div class="comment-title">
            <#if baseCommentURL?exists>
              <a href="${(baseCommentURL + '#comment-' + comment.ID?c)?html}">${comment.title?html}</a>
              <#else>
                ${comment.title?html}
              </#if>
          </div>
        </#if>
        <div class="comment-body">
          ${comment.text?html}
        </div>
        <div class="comment-info">
          ${comment.author?html} |
          <@vrtx.date value=comment.time format='long' />
          <#if deleteCommentURLs[comment.ID?c]?exists>
            (&nbsp;<a onclick="return confirm('Are you sure?');" href="${deleteCommentURLs[comment.ID?c]?html}">delete</a>&nbsp;)
          </#if>

        </div>
      </div>
      <#assign rowclass><#if rowclass="even">odd<#else>even</#if></#assign>
    </#list>
  </#if>

  <div class="add-comment" id="comment-form">
    <hr /> 

    <div class="add-comment-header"><@vrtx.msg code="commenting.form.add-comment" default="Add comment" /></div>

    <#if !postCommentURL?exists && !principal?exists && loginURL?exists>
      <#assign defaultMsg>
        To comment on this resource you have to <a href="${loginURL?html}">log in</a>
      </#assign>
      <p><@vrtx.rawMsg code="commenting.not-logged-in"
                         default=defaultMsg
                         args=[loginURL] /></p>

    <#elseif principal?exists && !commentsEnabled>
      <p><@vrtx.msg code="commenting.disabled"
                    default="Commenting is disabled on this resource." /></p>

    <#elseif principal?exists && !postCommentURL?exists>
      <p><@vrtx.msg code="commenting.denied"
                    default="You do not have permissions to add comments on this resource." /></p>

    <#elseif postCommentURL?exists>
      <form action="${postCommentURL?string?html}" method="post">
        <#if config.titlesEnabled>
        <p>
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
        </p>
        </#if>
        <p>
          <#assign value><#if form?exists && form.text?exists>${form.text}</#if></#assign>
          <textarea id="comments-text" name="text" rows="6" cols="80">${value}</textarea>
          <#if errors?exists && errors.getFieldError('text')?exists>
            <span class="error">
              <@vrtx.msg code=errors.getFieldError('text').getCode()
                         default=errors.getFieldError('text').getDefaultMessage() />
            </span>
          </#if>
        </p>
        <input type="submit" name="save" value="<@vrtx.msg code='commenting.form.submit' default='Submit' />" />
      </form>
    </#if>
  </div>


</div>
