<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
 <xsl:output method="html" doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN" doctype-system="http://www.w3.org/TR/html4/loose.dtd"/>
    
   <!-- ======================= VARIABLES ================================-->
    <xsl:variable name="missingCount" select="count(//missing)"/>
    <xsl:variable name="olderCount" select="count(//older)"/>
    <xsl:variable name="newerCount" select="count(//newer)"/>
    <xsl:variable name="corruptedCount" select="count(//corrupted)"/>
    <xsl:variable name="superfluousCount" select="count(//superfluous)"/>
    
    
   <!-- ======================= PROCESSING RULES ================================-->

    <!-- starting template processing -->
    <xsl:template match="/">
        <HTML>
            <HEAD>
                <META HTTP-EQUIV="CONTENT-TYPE" CONTENT="text/html; charset=iso-8859-1"/>
                <TITLE>3M::Mandriva Mirror Monitoring</TITLE>
                <LINK rel="stylesheet" type="text/css" href="../css/diff-main.css"></LINK>
            </HEAD>
            <BODY>
                <div id="header">
                    <span id="title">Status of mirror <i><xsl:value-of select="diff/@name"/></i></span>
                </div>
                
                <!-- some stats -->
                <xsl:call-template name="stats"/>
                
                <!-- data -->    
                <xsl:call-template name="missing"/>
                <xsl:call-template name="older"/>
                <xsl:call-template name="newer"/>
                <xsl:call-template name="corrupted"/>
                <xsl:call-template name="superfluous"/>
                
            </BODY>
        </HTML>
 
        
    </xsl:template>
    
    
    <!--
    ========================================================================
        DEFAULT PROCESSING (NODES)
    ========================================================================
    --> 
    <xsl:template match="missing|superfluous|corrupted">
        <li><xsl:value-of select="@path"/></li>
    </xsl:template>
    
    <xsl:template match="older|newer">
        <tr>
            <td><xsl:value-of select="@path"/></td>
            <td><xsl:value-of select="@expected"/></td>
            <td><xsl:value-of select="@effective"/></td>
        </tr>
    </xsl:template>
    
    <!--
    ========================================================================
        MISSING FILES
    ========================================================================
       -->
    <xsl:template name="missing">
        <div id="missing">
            <p class="title">Missing files</p>
            <ul>
                <xsl:apply-templates select="//missing"/>
            </ul>
        </div>
    </xsl:template>


    <!-- 
    ========================================================================
        SUPERFLUOUS FILES
    ========================================================================    
    -->
    <xsl:template name="superfluous">
        <div id="superfluous">
            <p class="title">Extra files <i>not</i> in the master</p>            
            <ul>
                <xsl:apply-templates select="//superfluous"/>
            </ul>
        </div>
    </xsl:template>
    

    <!-- 
    ========================================================================
        CORRUPTED FILES
    ========================================================================    
    -->
    <xsl:template name="corrupted">
        <div id="corrupted">
            <p class="title">Files corrupted (which size is not matching)</p>            
            <ul>
                <xsl:apply-templates select="//corrupted"/>
            </ul>
        </div>
    </xsl:template>
    
    
    <!-- 
    ========================================================================
        OLDER FILES
    ========================================================================    
    -->
    <xsl:template name="older">
        <div id="older">
            <p class="title">Files with an older time stamp</p>  
            <table>
                <tr>
                    <th>Path</th>
                    <th>Expected time stamp</th>
                    <th>Effective time stamp</th>
                </tr>
                <xsl:apply-templates select="//older"/>
            </table>  
        </div>
    </xsl:template>


    <!-- 
    ========================================================================
        NEWER FILES
    ========================================================================    
    -->
    <xsl:template name="newer">
        <div id="newer">
            <p class="title">Files with a more recent time stamp on the mirror</p>   
            <table>
                <tr>
                    <th>Path</th>
                    <th>Expected time stamp</th>
                    <th>Effective time stamp</th>
                </tr>
                <xsl:apply-templates select="//newer"/>
            </table>    
        </div>
    </xsl:template>
    
    
    <!-- 
        ========================================================================
        STATISTICS
        ========================================================================    
    -->
    <xsl:template name="stats">
        <div id="stats">
            <p class="title">Statistics Summary</p> 
            <table>
                <tr>
                    <td>Missing files</td><td><xsl:value-of select="$missingCount"/></td>
                </tr>
                <tr>
                    <td>Extra files (superfluous)</td><td><xsl:value-of select="$superfluousCount"/></td>
                </tr>
                <tr>
                    <td>Older files</td><td><xsl:value-of select="$olderCount"/></td>
                </tr>
                <tr>
                    <td>Newer files</td><td><xsl:value-of select="$newerCount"/></td>
                </tr>
                <tr>
                    <td>Corrupted files</td><td><xsl:value-of select="$corruptedCount"/></td>
                </tr>
            </table>
        </div>
    </xsl:template>        
        
</xsl:stylesheet>