<%@page contentType="text/html" pageEncoding="UTF-8"%>
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
        <section>
            <c:if test="${param.fail}">
                Incorrect username/password, please try again.<br />
            </c:if>
            <form action="checklogin" method="post">
                <table>
                    <tr>
                        <td>Username:</td>
                        <td><input type="text" name="username" autofocus></td>
                    </tr>
                    <tr>
                        <td>Password:</td>
                        <td><input type="password" name="password" ></td>
                    </tr>
                </table>
                <input type="submit"  value="Log in">
            </form>
        </section>
    </body> 
</html>