<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                xmlns:xhtml="http://www.w3.org/1999/xhtml" version="1.0">
  <xsl:output
      doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
      doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"
      omit-xml-declaration="yes"
      method="xml"
      indent="yes"
      media-type="text/html"/>

  <xsl:template match="/">
    <xsl:apply-templates />
  </xsl:template>
 
  <xsl:template match="*">
   <xsl:copy>
      <xsl:copy-of select="@*[name()!='xml:space' and name()!='xml:lang']"/>
      <xsl:apply-templates /> 
    </xsl:copy>
  </xsl:template>

  <xsl:template match="comment() | processing-instruction()">
    <xsl:copy />
  </xsl:template> 

  <xsl:template match="text()">
    <xsl:value-of select="normalize-space(.)" />
  </xsl:template>

  <!-- specify which elements that should preserve space -->
  <xsl:template match="xhtml:code//text() |
                       xhtml:pre//text() | 
                       xhtml:style//text() |
                       xhtml:script//text()">
    <xsl:value-of select="." />
  </xsl:template>


</xsl:stylesheet>
