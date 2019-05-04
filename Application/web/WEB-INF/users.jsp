<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<tags:page-template>
    <jsp:body>

		<%-- 
			status - information about page loading process
		--%>

        <script>
			function ActionUser() {
				var id = document.getElementById("usersForm").elements["id"].value;
				document.deviceForm.action = document.deviceForm.action + "/" + id;
				return true;
			}
        </script>

		<c:if test="${loadFail eq true}">
			Loading users failed.
		</c:if>

        <c:if test="${loadFail ne true}">

			<c:if test="${not empty users}">
				Users:<br />
				<table>
					<tr>
						<th>Username</th>
						<th>Level</th>
						<th>Password</th>

					</tr>
					<c:forEach items="${users}" var="user">
						<tr>
						<form action="${pageContext.request.contextPath}/users" method="post" 
							  <input type="hidden" name="reqtype" value="change" >
							<input type="hidden" name="username" value ="${user.name}" >
							<td>${user.name}</td>
							<td>			
								<select name="level">
									<c:forEach items="${levels}" var="entry">
										<option value="${entry.key}" 
												<c:if test="${entry.key eq user.level}">selected="selected"</c:if>
												>${entry.value}</option>
									</c:forEach>
								</select>
							</td>
							<td><input type="password" name="password"></td>
							<td><input type="submit" name="change_button" value="Change">
								<input type="submit" name="delete_button"  value="Delete"></td>
						</form>
					</tr>
				</c:forEach> 
			</table><br />
			Keep password column empty to change just user level.<br />
		</c:if>
	</c:if>

	<hr />
	Create user:<br />
	<form name="usersForm" action="/users" method="post" id="usersForm">

		<table>
			<tr>
				<td>Username:</td><td><input type="text" name="username" required="required"><td>
			</tr>
			<tr>
				<td>Password:</td><td><input type="password" name="password" required="required"><td>
			</tr>
			<tr><td>Level</td><td><select name="level">
						<option value="user" selected="selected">User</option>
						<option value="moderator">Moderator</option>
						<option value="admin">Administrator</option>
					</select> </td></tr>
		</table> 
		<input type="submit"  name="create_button" value="Create" <%--onclick="return ActionUser();"--%>>
	</form><br />

	<c:choose>
		<c:when test="${status eq 'modFail'}">
			Modification of chosen user failed, try again.
		</c:when>
		<c:when test="${status eq 'delFail'}">
			Deletion failed, try again.
		</c:when>
		<c:when test="${status eq 'creFail'}">
			User creation failed, try again.
		</c:when>
		<c:when test="${status eq 'inpFail'}">
			Incorrect input values for username or password.
		</c:when>
	</c:choose>

</jsp:body>
</tags:page-template>
