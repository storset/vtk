<#import "/lib/vortikal.ftl" as vrtx />

<div class="vrtx-comments" id="comments">

  <#if comments?exists>
    <hr />
    <#assign header>
      <@vrtx.msg code="commenting.header"
                 default="Comments (" + comments?size + ")"
                 args=[comments?size] />
    </#assign>
    <#if baseCommentURL?exists>
      <#-- XXX: which header level to use? -->
      <h3><a href="${(baseCommentURL + '#comments')?html}">${header}</a></h3>
    <#else>
      ${header}
    </#if>

    <#assign comments = comments?sort_by("time") />

    <#list comments as comment>
      <p class="vrtx-comment" id="comment-${comment.ID?c}">
        <#if config.titlesEnabled>
          <div class="comment-title">
            <#if baseCommentURL?exists>
              <a href="${(baseCommentURL + '#comment-' + comment.ID?c)?html}">${comment.title?html}</a>
              <#else>
                ${comment.title?html}
              </#if>
              <#if deleteCommentURLs[comment.ID?c]?exists>
                (&nbsp;<a onclick="return confirm('Are you sure?');" href="${deleteCommentURLs[comment.ID?c]?html}">delete</a>&nbsp;)
              </#if>
          </div>
        </#if>
        <span class="comment-body">${comment.text?html}</span>
        <div class="comment-info">
          ${comment.author?html} |
          <@vrtx.date value=comment.time format='long' />
        </div>
      </p>
    </#list>
  </#if>

    <div class="add-comment" id="comment-form">
      <hr /> 

      <#-- XXX: which header level to use? -->
      <h3><@vrtx.msg code="commenting.form.add-comment" default="Add comment" /></h3>

      <#if !postCommentURL?exists && !principal?exists && loginURL?exists>
        <#assign defaultMsg>
          To comment on this resource you have to <a href="${loginURL?html}">log in</a>
        </#assign>
        <p><@vrtx.rawMsg code="commenting.not-logged-in"
                         default=defaultMsg
                         args=[loginURL] /></p>
      </#if>

      <#if principal?exists && !postCommentURL?exists>
        <p><@vrtx.msg code="commenting.denied"
                      default="You do not have permissions to add comments on this resource." />
        </p>

      <#elseif postCommentURL?exists>
      <form action="${postCommentURL?string?html}" method="post">
        <#if config.titlesEnabled>
        <p>
          <#assign value><#if form?exists && form.title?exists>${form.title}</#if></#assign>
          <label for="comments-title">
            <@vrtx.msg code="commenting.form.title" default="Title" /></label>
          <input style="display:block;" id="comments-title" type="text" name="title" size="30" value="${value}" />
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
          <!--label for="comments-text"><@vrtx.msg code="commenting.form.text" default="Comment" /></label-->
          <textarea style="display:block;"  id="comments-text" name="text" rows="5" cols="40">${value}</textarea>
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
