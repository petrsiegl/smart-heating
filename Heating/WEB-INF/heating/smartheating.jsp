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
            <div class="table">
                <div class="tr">
                    <div class="th">Heating unit id</div>
                    <div class="th">Thermometer id</div>
                    <div class="th">Relay id</div>
                    <div class="th">Current temperature</div> 
                    <div class="th">Target temperature</div>
                </div>
                <c:forEach items="${units}" var="entry">
                    <div class="tr">
                        <div class="td">${entry.key}</div>
                        <div class="td">${entry.value.thermometer.ID}</div>
                        <div class="td">${entry.value.relay.ID}</div>
                        <div class="td">${entry.value.thermometer.getTemperature()}</div>

					<form action="/${prefix}/home" method="get">
                        <div class="td"><input type="text" name="temp" value="${entry.value.preferredTemp}"></div>

                        <div class="td">
							<input type="hidden" name="reqtype" value="change" >
							<input type="hidden" name="id" value ="${entry.key}" >
							<input type="submit" name="set_button" value="Set">
							<c:if test="${sessionScope.user.level >= 50}">
								<input type="submit" name="delete_button"  value="Delete">
							</c:if>
						</div>

                    </form>
                </div>
            </c:forEach> 
        </div>
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

	<c:if test ="${not empty status}">
		${status}<br />
	</c:if>

</jsp:body>
</tags:heating-template>

