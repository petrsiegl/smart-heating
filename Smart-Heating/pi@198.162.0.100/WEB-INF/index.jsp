<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="tags" tagdir="/WEB-INF/tags" %>
 <%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<tags:page-template>
    <jsp:body>
        <p>
        <jsp:useBean id="today" class="java.util.Date" scope="page" />
        Today's date: <fmt:formatDate value="${today}" pattern="hh:mm MM.dd.yyyy" />
        </p>
    </jsp:body>
</tags:page-template>
