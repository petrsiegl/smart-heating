<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<tags:page-template>
    <jsp:body>

		<%-- 
			status - information about page loading process
		--%>

		<c:set var="lastUser" value="${fn:length(users) eq 1}" ></c:set>

			<script>
				function ActionUser() {
					var id = document.usersForm.elements["username"].value;
					document.usersForm.action = document.usersForm.action + "/" + id;
					return true;
				}

				function ChangeUser() {
					var id = document.changeForm.elements["username"].value;
					document.changeForm.action = document.changeForm.action + "/" + id;
					return true;
				}
			</script>

		<c:if test="${loadFail eq true}">
			Loading users failed.
		</c:if>

        <c:if test="${loadFail ne true}">

			<c:if test="${not empty users}">
				Users:<br />
				<div class="table users">
					<div class="tr">
						<div class="td th">Username</div>
						<div class="td th">Level</div>
						<div class="td th">Password</div>
						<div class="td th">&darr;</div>


					</div>
					<c:forEach items="${users}" var="user">
						<form name="changeForm" class="tr" action="${pageContext.request.contextPath}/users" method="post" >
							<input type="hidden" name="username" value ="${user.name}" />
							<div class="td">${user.name}</div>
							<div class="td">			
								<select name="level">
									<c:forEach items="${levels}" var="entry">
										<option value="${entry.key}" 
												<c:if test="${entry.key eq user.level}">selected="selected"</c:if>
												>${entry.value}</option>
									</c:forEach>
								</select>
							</div>
							<div class="td"><input type="password" name="password"></div>
							<div class="td">
								<input type="submit" name="modify_button" value="Modify"  onclick="return ChangeUser();">
								<c:if test="${lastUser ne true}">
									<input type="submit" name="delete_button"  value="Delete"  onclick="return ChangeUser();">
								</c:if>
							</div>
						</form>
					</c:forEach> 
				</div><br />
				Keep password column empty to change just user level.<br />
				<c:if test="${lastUser eq true}">
					At least one user must exist.<br />
				</c:if>

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
			<input type="submit"  name="create_button" value="Create" onclick="return ActionUser();">
		</form><br />

		<c:if test="${not empty status}">
			${status}<br />
		</c:if>

	</jsp:body>
</tags:page-template>
