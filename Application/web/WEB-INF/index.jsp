<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<tags:page-template>
    <jsp:body>
        <p>
			<jsp:useBean id="today" class="java.util.Date" />
			<fmt:formatDate var="date" value="${today}" pattern="dd.MM.yyyy" />
			Today's date: ${date}
        </p>

    </jsp:body>
</tags:page-template>
