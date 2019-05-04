<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<tags:page-template>
    <jsp:body>
		Change password to this account: <br />
        <form action="/account" method="post" >
            New password: <input type="password" name="password" required="required">
            <input type="submit"  value="change">
        </form> <br />
		<c:if test="${not empty status}">
			Request status:<br />
			<c:choose>
				<c:when test="${status == 'changed'}">
					Password has been changed. <br />
				</c:when>

				<c:when test="${status == 'failed'}">
					Problem occurred, try again. <br />
				</c:when>
			</c:choose>
		</c:if>
    </jsp:body>
</tags:page-template>
