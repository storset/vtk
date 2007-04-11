<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output
      doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
      doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"
      omit-xml-declaration="yes"
      method="html"
      indent="yes"
      media-type="text/html"/>

  <xsl:strip-space elements="*" />
  <!--xsl:preserve-space elements="pre"/-->

  <!--
  <xsl:template match="/">
    <xsl:copy-of select="."/>
  </xsl:template>
  -->

  <xsl:template match="*">
    <xsl:copy>
      <xsl:copy-of select="@*"/>
      <xsl:apply-templates />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="comment()|processing-instruction()">
    <xsl:copy />
  </xsl:template>

</xsl:stylesheet>
