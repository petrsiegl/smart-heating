<%@tag description="Page template" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html> 
    <head>
        <link rel="stylesheet" type="text/css" href="/resources/css/styles.css">
        <meta charset="utf-8">
    </head>
    <body>
        <header>
            <jsp:include page="/WEB-INF/header.jsp" />
        </header>
        <nav class="login">
            <jsp:include page="/WEB-INF/menu.jsp" />
            <c:if test="${user != null}">
                <a href='/logout'>Log out</a>
            </c:if>

        </nav>
        <section>
            <jsp:doBody/>
        </section>
    </body> 
</html>

