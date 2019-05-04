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
		<c:choose>
			<c:when test="${param.result=='pinExists'}">
				Device connected to that pin already exists.<br />
			</c:when>
			<c:when test="${param.result=='idExists'}">
				Device with that ID already exists.<br />
			</c:when>
			<c:when test="${param.result=='success'}">
				Device successfully created.<br />
			</c:when>
			<c:when test="${param.result=='fail'}">
				Incorrect data entered.<br />
			</c:when>
		</c:choose>
	</jsp:body>
</tags:page-template>
