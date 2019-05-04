<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<tags:page-template>
    <jsp:body>
        <%-- Print info only if there was query for id --%>
        <c:if test="${not empty param.id}">
            <c:choose>
                <%-- found true means device was found correctly --%>
                <c:when test="${found}">
                    ID: ${param.id} <br />
                    <form action="/servlet/updatedevice" method="get">
                        <input type="hidden" name="id" value="${param.id}">
                        <%-- Print out every piece of information about device --%>
                        <c:forEach items="${info}" var="entry">
                            ${entry.key} : ${entry.value} <br />
                        </c:forEach>
                        <%-- For relays print ON OFF radio buttons --%>
                        <c:forEach items="${relaystates}" var="entry">
                            ${entry.key} : 
                            <input type="radio" name="${entry.key}"
                                   value="ON" 
                                   <c:if test="${entry.value == 'ON'}">
                                       checked
                                   </c:if>
                                   > ON 
                            <input type="radio" name="${entry.key}"
                                   value="OFF" 
                                   <c:if test="${entry.value == 'OFF'}">
                                       checked
                                   </c:if>
                                   > OFF<br />

                        </c:forEach>

                        <input type="submit" name="set_button" value="Set">
                        <input type="submit" name="delete_button"  value="Delete">
                    </form>
                </c:when>
                <c:otherwise>
                    Device with ID ${param.id} not found.
                </c:otherwise>
            </c:choose>
        </c:if>

        <hr />
        Choose device:<br />
        <form action="devices.jsp" method="get">
            <select name="id">
                <%-- ids = ids of all devices --%>
                <c:forEach items="${ids}" var="id">
                    <option value="${id}">${id}</option>"
                </c:forEach>
            </select>
            <input type="submit"  value="Show">
        </form>
    </jsp:body>
</tags:page-template>
