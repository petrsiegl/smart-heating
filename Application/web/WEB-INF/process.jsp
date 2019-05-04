<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<tags:page-template>
    <jsp:body>
        <hr />
        Choose process:<br />
        <form action="process.jsp" method="get">
            <select name="id">
                <c:forEach items="${ids}" var="id">
                    <option value="${id}">${id}</option>
                </c:forEach>
            </select>
            <input type="submit"  value="Show">
        </form>
    </jsp:body>
</tags:page-template>
