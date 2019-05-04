<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<tags:page-template>
    <jsp:body>
        <form action="/servlet/adddevice" method="get">        

            ID of device:<br />
            <input type="text" name="id">

            <select name="type">
                <c:forEach items="${types}" var="type">
                    <option value="${type}">${type}</option>
                </c:forEach>
            </select>

            <select name="pin">
                <c:forEach items="${pins}" var="pin">
                    <option value="${pin}">${pin}</option>
                </c:forEach>
            </select>
            <input type="submit"  value="Create">
        </form>
    </jsp:body>
</tags:page-template>
