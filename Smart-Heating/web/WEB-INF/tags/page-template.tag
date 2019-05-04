<%@tag description="Page template" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@attribute name="head" fragment="true" %>
<!DOCTYPE html>
<html> 
    <head>
		<title>Smart Home</title>
        <link rel="stylesheet" type="text/css" href="/resources/css/styles.css">
        <meta charset="utf-8">
		<meta name="viewport" content="width=device-width, initial-scale=1.0">
		<jsp:invoke fragment="head"/>
    </head>
    <body>
        <header>
            <jsp:include page="/WEB-INF/header.jsp" />
        </header>
        <nav class="login">
            <jsp:include page="/WEB-INF/menu.jsp" />
            <c:if test="${sessionScope.user != null}">
                <ul>
					<c:if test="${sessionScope.user.level >= 100}">
						<li><a href='/users'>Users</a></li>
						</c:if>
                    <li><a href='/account'>Account</a></li>
                    <li><a href='/logout'>Log out</a></li>   
                </ul>
            </c:if>

        </nav>
        <section>
            <jsp:doBody/>
        </section>
    </body> 
</html>

