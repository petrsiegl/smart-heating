<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<tags:heating-template>
	<jsp:attribute name="head">
		<meta http-equiv="refresh" content="60" />
    </jsp:attribute>
    <jsp:body>

        <%-- units is map String(id),HeatingUnit   --%>
        <c:if test="${not empty units}">
            Heating units:<br />
            <table>
                <tr>
                    <th>Heating unit id</th>
                    <th>Thermometer id</th>
                    <th>Relay id</th>
                    <th>Current temperature</th> 
                    <th>Target temperature</th>
                </tr>
                <c:forEach items="${units}" var="entry">
                    <tr>
                        <td>${entry.key}</td>
                        <td>${entry.value.thermometer.ID}</td>
                        <td>${entry.value.relay.ID}</td>
                        <td>${entry.value.thermometer.getTemperature()}</td>

					<form action="/${prefix}/home" method="get">
                        <td><input type="text" name="temp" value="${entry.value.preferredTemp}"></td>

                        <td>
							<input type="hidden" name="reqtype" value="change" >
							<input type="hidden" name="id" value ="${entry.key}" >
							<input type="submit" name="set_button" value="Set">
							<c:if test="${sessionScope.user.level >= 50}">
								<input type="submit" name="delete_button"  value="Delete">
							</c:if>
						</td>

                    </form>
                </tr>
            </c:forEach> 
        </table>
    </c:if><br />


    Current heating mode: ${currentMode}
	<c:if test="${sessionScope.user.level >= 50}">
		<form action="/${prefix}/home" method="get">
			<select name="mode">
				<c:forEach items="${modes}" var="entry">
					<option value="${entry}">${entry}</option>
				</c:forEach> 
			</select>
			<button type="submit" name="change_button" value="Change">Change</button>
		</form>
	</c:if>
	<c:if test ="${status eq 'modFail'}">
		Mode change failed.
	</c:if>

	<c:if test ="${status eq 'tempFail'}">
		Invalid temperature input.
	</c:if>

	<c:if test ="${status eq 'deleted'}">
		Unit deleted.
	</c:if>

</jsp:body>
</tags:heating-template>

