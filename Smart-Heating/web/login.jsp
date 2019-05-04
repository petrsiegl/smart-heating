<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html> 
    <head>
        <link rel="stylesheet" type="text/css" href="/resources/css/styles.css">
        <meta charset="utf-8">
    </head>
    <body id="body-login">
		<div class="login-block">
			<header>
				Smart Home
			</header>
			<section>
				<c:if test="${param.fail}">
					Incorrect username/password, please try again.<br />
				</c:if>
				<form action="checklogin" method="post">
					<input type="text" name="username" placeholder="Username" autofocus required><br />
					<input type="password" name="password" placeholder="Password" required><br />
					<input type="submit"  value="Log in">
				</form>
			</section>
		</div>
    </body> 
</html>