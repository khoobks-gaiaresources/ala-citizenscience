<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>

<h1>Manage Portals</h1>
<cw:getContent key="root/portalListing" />

<div class="input_container">
	<div class="buttonpanel textright">
	    <input class="form_action" type="button" value="Add Portal" onclick="window.document.location='${portalContextPath}/bdrs/root/portal/edit.htm'"/>
	</div>
	
	<table class="datatable textcenter">
	    <thead>
	        <tr>
	            <th>Portal Name</th>
                <th>URL Prefix</th>
	            <th>Is Default</th>
	            <th>Is Active</th>
	        </tr>
	    </thead> 
	    <tbody>
	        <c:forEach var="portal" items="${portalList}">
	            <tr>
	                <td>
	                    <a href="${portalContextPath}/bdrs/root/portal/edit.htm?id=${ portal.id }">
	                        <c:out value="${ portal.name }"/>
	                    </a>
	                </td>
                    <td>
                        <c:out value="${portal.urlPrefix}"/>
                    </td>
	                <td>
	                    <c:choose>
	                        <c:when test="${ portal.default }">
	                            Yes
	                        </c:when>
	                        <c:otherwise>
	                            &nbsp;
	                        </c:otherwise>
	                    </c:choose>
	                </td>
	                <td>
                        <c:choose>
                            <c:when test="${ portal.active }">
                                Yes
                            </c:when>
                            <c:otherwise>
                                &nbsp;
                            </c:otherwise>
                        </c:choose>
                    </td>
	            </tr>
	        </c:forEach>
	    </tbody>
	</table>
</div>
