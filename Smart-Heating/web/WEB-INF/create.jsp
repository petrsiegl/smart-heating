<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<tags:page-template>
    <jsp:body>
        <%--
            types: All possible types of device
            pins: All possible gpio pins to connect with device
        --%>


		<form action="/create-device" method="get">     
			<table>
				<tr>
					<td>Identifier</td>
					<td><input type="text" name="id" required="required"></td>
				</tr>

				<tr>
					<td>Type</td>
					<td>
						<select name="type">
							<c:forEach items="${types}" var="type">
								<option value="${type}">${type}</option>
							</c:forEach>
						</select>
					</td>
				</tr>

				<tr><td>Pin</td><td>
						<select name="pin">
							<c:forEach items="${pins}" var="pin">
								<option value="${pin}">${pin}</option>
							</c:forEach>
						</select></td></tr></table>
			<input type="submit"  value="Create">
		</form> <br />
		
		<c:if test="${not empty status}">
			${status}<br />
		</c:if>

	</jsp:body>
</tags:page-template>
