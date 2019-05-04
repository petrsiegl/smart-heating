<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<tags:heating-template>
    <jsp:body>

        <form action="/heating/predefined" method="get">
			<table>
				<tr>
					<td>Day of week:</td>
					<td>
						<select name="day">
							<c:forEach items="${days}" var="dayEntry">
								<option value="${dayEntry}">${dayEntry}</option>
							</c:forEach>
						</select>
					</td>
				</tr>
				<tr>
					<td>Unit ID:</td>
					<td>
						<select name="heatingUnitID">
							<c:forEach items="${heatingUnits}" var="hu">
								<option value="${hu}">${hu}</option>
							</c:forEach>
						</select>
					</td>
				</tr>
			</table>
            <button type="submit" name="show_button"  value="Show">Show</button>
        </form>

		<hr />

        <c:if test="${not empty tableRecords}">

			Day: ${day}<br />

			Heating Unit ID: ${unitID}<br />

			<table>
				<tr>
					<th>Time</th><th>Temperature</th>
				</tr>
				<c:forEach items="${tableRecords}" var="record">
					<tr>
						<td>${record.formatedTime}</td>
						<td>${record.temperature}</td>
						<td>				
							<form>
								<input type="hidden" name="day" value="${day}">
								<input type="hidden" name="id" value="${unitID}">
								<input type="hidden" name="time" value="${record.formatedTime}">
								<c:if test="${sessionScope.user.level >= 50}">
								<button type="submit" name="delete_button" value="Delete">Delete</button>
								</c:if>
							</form>
						</td>
					</tr>
				</c:forEach>
			</table>

		</c:if>
		<c:if test="${show eq true and  empty tableRecords}">
			No settings found.
		</c:if>

		<br />
		<c:if test="${sessionScope.user.level >= 50}">
		Add new setting:
		<%-- ${pageContext.request.contextPath} --%>
		<form action="/heating/predefined">
			<table>
				<tr>
					<td>Time:</td>
					<td>
						<select name="addTime">
							<c:forEach items="${availibleTimes}" var="entry">
								<option value="${entry}">${entry}</option>
							</c:forEach><br />
						</select>
					</td>
				</tr>
				<tr>
					<td>Heating Unit:</td>
					<td>
						<select name="addUnitID">
							<c:forEach items="${heatingUnits}" var="hu">
								<option value="${hu}">${hu}</option>
							</c:forEach><br />
						</select>
					</td>
				</tr>
				<tr>
					<td>Day:</td>
					<td>
						<select name="addDay">
							<c:forEach items="${days}" var="dayEntry">
								<option value="${dayEntry}">${dayEntry}</option>
							</c:forEach>
						</select>
					</td>
				</tr>
				<tr>
					<td>Temperature:</td>
					<td><input name="temperature" type="text" /> </td>
				</tr>
			</table>
			<button type="submit" name="add_button" value="Add">Add</button>

		</form>
		</c:if><br />

		<c:if test="${status eq 'numFail'}">
			Invalid temperature value input.
		</c:if>
		<c:if test="${status eq 'dayFail'}">
			Invalid day input.
		</c:if>
		<c:if test="${status eq 'unitFail'}">
			Unit not found.
		</c:if>
		<c:if test="${status eq 'added'}">
			Record added.
		</c:if>

		<c:if test="${status eq 'remFail'}">
			Record removing failed, try again.
		</c:if>
	</jsp:body>
</tags:heating-template>

