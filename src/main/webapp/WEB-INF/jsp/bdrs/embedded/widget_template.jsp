<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page import="java.net.URLEncoder" %>

<html class="embed">
    <head>
        <%@page import="au.com.gaiaresources.bdrs.model.theme.Theme"%>

        <c:set var="portalContextPath" scope="request" value="${pageContext.request.contextPath}${currentPortal.portalContextPath}"/>

        <script src="${pageContext.request.contextPath}/js${version}/jquery-1.6.2.min.js" type="text/javascript"></script>
        
        <c:choose>
            <c:when test="${ theme != null }">
                <c:forEach items="${ theme.cssFiles }" var="cssFile">
                    <link rel="stylesheet" href="${portalContextPath}/files/download.htm?className=au.com.gaiaresources.bdrs.model.theme.Theme&id=${ theme.id }&fileName=<%= Theme.THEME_DIR_PROCESSED %>/${ cssFile }" type="text/css">
                </c:forEach>
                <c:forEach items="${ theme.jsFiles }" var="jsFile">
                    <script type="text/javascript" src="${portalContextPath}/files/download.htm?className=au.com.gaiaresources.bdrs.model.theme.Theme&id=${ theme.id }&fileName=<%= Theme.THEME_DIR_PROCESSED %>/${ jsFile }"></script>
                </c:forEach>
            </c:when>
            <c:otherwise>
                <!-- Reset all browser specific styles -->
                <link rel="stylesheet" href="${pageContext.request.contextPath}/css${version}/yui3/yui3-reset.css" type="text/css">
                <link rel="stylesheet" href="${pageContext.request.contextPath}/css${version}/yui3/yui3-fonts.css" type="text/css">
                <link rel="stylesheet" href="${pageContext.request.contextPath}/css${version}/yui3/yui3-base.css" type="text/css">
                
                <!-- The BDRS core default styles -->
                <link rel="stylesheet" href="${pageContext.request.contextPath}/css${version}/bdrs/bdrs.css" type="text/css"/>

                <link rel="stylesheet" href="${pageContext.request.contextPath}/css${version}/ketchup/jquery.ketchup.css" type="text/css"/>
                
                <script type="text/javascript" src="${pageContext.request.contextPath}/js/jquery-ui-1.8.11.custom.min.js"></script>
                <link type="text/css" href="${pageContext.request.contextPath}/css${version}/redmond/jquery-ui-1.8.11.custom.css" rel="stylesheet" />
            </c:otherwise>
        </c:choose>
        
        <c:forEach var="cssFile" items="${customCSS}">
            <link rel="stylesheet" href="${pageContext.request.contextPath}/css${version}/${cssFile}" type="text/css"/>
        </c:forEach>
        
        <link rel="stylesheet" href="${pageContext.request.contextPath}/js${version}/colorpicker/css/colorpicker.css" type="text/css"/>
        <script src="${pageContext.request.contextPath}/js${version}/colorpicker/js/colorpicker.js" type="text/javascript"></script>
        
        <script src="${pageContext.request.contextPath}/js${version}/jquery.ui.autocomplete.html.js" type="text/javascript"></script>
        
        <script type="text/javascript" src="${pageContext.request.contextPath}/js${version}/ketchup/jquery.ketchup.js"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath}/js${version}/ketchup/jquery.ketchup.messages.js"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath}/js${version}/ketchup/jquery.ketchup.validations.basic.js"></script>
        
        <script type="text/javascript" src="${pageContext.request.contextPath}/js${version}/jquery.cj-simple-slideshow.js"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath}/js${version}/jquery.tablednd_0_5.js"></script>
        
        <!--  JqGrid stuff the grid.local-en.js file needs t come before the jqGrid.min.js file -->
        <link rel="stylesheet" type="text/css" media="screen" href="${pageContext.request.contextPath}/js${version}/jquery.jqGrid-4.0.0/css/ui.jqgrid.css" />
        <script src="${pageContext.request.contextPath}/js${version}/jquery.jqGrid-4.0.0/js/i18n/grid.locale-en.js" type="text/javascript"></script>
        <script src="${pageContext.request.contextPath}/js${version}/jquery.jqGrid-4.0.0/js/jquery.jqGrid.min.js" type="text/javascript"></script>
        
        <script src="${pageContext.request.contextPath}/js${version}/jquery-tmpl/jquery.tmpl.js" type="text/javascript"></script>
                
        <c:if test="${maps == true}">
            <script src="${pageContext.request.contextPath}/js${version}/ol/OpenLayers.js" type="text/javascript"></script>
            <script src="http://maps.google.com/maps?file=api&amp;v=2&amp;&amp;sensor=false&amp;key=${bdrsGoogleMapsKey}" type="text/javascript"></script>
            <script src="${pageContext.request.contextPath}/js${version}/BdrsCluster.js" type="text/javascript"></script>
        </c:if>
        
        <script type="text/javascript" src="${pageContext.request.contextPath}/js${version}/bdrs.js"></script>
		
		<%-- putting the JSP in one line so the query string doesn't get filled with spaces.
		     I assume the query parameter names were being padded with spaces so when it came time
			 for JSP to do its replace work nothing happened and we had no styling in the embedded widgets
		 --%>
		<link rel="stylesheet" href="${portalContextPath}/bdrs/public/embedded/bdrs-embed.css.htm?a=b<c:forEach var="entry" items="${ paramMap }"><jsp:useBean id="entry" type="java.util.Map.Entry"/>&<c:out value="${ entry.key }"/>=<%= URLEncoder.encode(entry.getValue().toString()) %></c:forEach>" type="text/css"/>
    </head>
    <body>
        <tiles:insertAttribute name="content"/>
        <jsp:useBean id="showFooter" type="java.lang.String" scope="request"/>
        <c:if test="<%= Boolean.parseBoolean(showFooter) %>">
            <div class="right" style="position:fixed; bottom: 0px;">
                <a href="http://${ domain }:${ port }${ contextPath }/home.htm" target="_target">
                    Biological Data Recording System
                </a>
            </div>
        </c:if>
    </body>
</html>