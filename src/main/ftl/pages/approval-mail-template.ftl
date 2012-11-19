<p>Hi!</p>

<p>Can you approve this?</p>

<h2>${title}</h2>

<#if comment?exists && comment?has_content>
<pre>${comment}</pre>
</#if>


<p>Link to document: <a href="${uri?html}">${uri?html}</a></p>

<p>Best regards, ${mailFrom}</p>

<hr />

<p>
This message is sent on behalf of ${mailFrom}.
Your email address will not be saved.
You will not receive further messages of this kind
unless someone sends another resource for approval.
</p>
