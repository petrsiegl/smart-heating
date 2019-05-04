<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<tags:page-template>
    <jsp:body>
        <hr />
        Rooms:<br />
        <form action="/servlet/editroom" method="get">
            <select name="id">
                <c:forEach items="${rooms}" var="room">
                    <option value="${room}">${room}</option>
                </c:forEach>
            </select>

            <input type="submit" name="set_button" value="Set">
            <input type="submit" name="delete_button"  value="Delete">
        </form>
    </jsp:body>
</tags:page-template>
