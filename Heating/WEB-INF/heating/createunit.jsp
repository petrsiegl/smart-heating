<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<tags:heating-template>
    <jsp:body>

		Create new heating unit:<br />
		<form action="/${prefix}/createunit" method="get">
			<table>
				<tr>
					<td>Unit ID</td>
					<td><input type="text" name="id" required></td>
				</tr>
				<tr>
					<td>Relay ID</td>
					<td><select name="idrelay">
							<c:forEach items="${relays}" var="id">
								<option value="${id}">${id}</option>"
							</c:forEach>  
						</select></td>            
				</tr>
				<tr>
					<td>Thermometer ID</td>
					<td><select name="idthermo">
							<c:forEach items="${thermometers}" var="id">
								<option value="${id}">${id}</option>"
							</c:forEach>           
						</select></td>
				</tr>

			</table>
			<button type="submit" name="create_button" value="Create">Create</button>

		</form><br />

		<c:if test ="${not empty status}">
			${status}<br />
		</c:if>
	</jsp:body>
</tags:heating-template>

