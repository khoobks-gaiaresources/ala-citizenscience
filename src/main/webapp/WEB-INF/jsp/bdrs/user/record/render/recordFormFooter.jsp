<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%-- required attributes --%>
<tiles:useAttribute name="recordWebFormContext" />  <%-- RecordWebFormContext --%>

<c:choose>
    <c:when test="${ recordWebFormContext.preview }">
        <div class="recordFormFooter buttonpanel textright">
            <input class="form_action" type="button" value="Go Back" onclick="window.document.location='${portalContextPath}/bdrs/admin/survey/editAttributes.htm?surveyId=${recordWebFormContext.surveyId}'"/>
            <input class="form_action" type="button" value="Continue" onclick="window.document.location='${portalContextPath}/bdrs/admin/survey/locationListing.htm?surveyId=${recordWebFormContext.surveyId}'"/>
        </div>
    </c:when>
    <c:otherwise>
        <c:choose>
            <c:when test="${ recordWebFormContext.editable }">
                <c:choose>
                    <c:when test="${ recordWebFormContext.existingRecord }">
                            <div class="recordFormFooter buttonpanel textright">
                                <input id="saveButton" class="form_action" type="submit" name="submit" value="Save Changes"/>
                            </div>
                        </form>
                    </c:when>
                    <c:otherwise>
                            <div class="recordFormFooter buttonpanel textright">
                                <input class="form_action" type="submit" name="submitAndAddAnother" value="Submit and Add Another"/>
                                <input class="form_action" type="submit" name="submit" value="Submit"/>
                            </div>
                        </form>
                    </c:otherwise>
                </c:choose>
            </c:when>
            <c:otherwise>
                <%-- Useful so we always have a node for a clearing div when we are viewing CSS layout forms in read only mode --%>
                <div class="recordFormFooter"></div>
            </c:otherwise>
        </c:choose>
        

    <tiles:insertDefinition name="commentsRenderer"/>

    </c:otherwise>
</c:choose>