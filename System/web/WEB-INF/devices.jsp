<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<tags:page-template>
    <jsp:body>
        <script>
			function ActionDevice() {
				var id = document.deviceForm.elements["id"].value;
				document.deviceForm.action = document.deviceForm.action + "/" + id;
				return true;
			}

			function ActionChange() {
				var id = document.changeForm.elements["id"].value;
				document.changeForm.action = document.changeForm.action + "/" + id;
				return true;
			}
        </script>

        <%--
            found: whether device that was asked for exists  
			info: map(String label, String text)
			radios: map(String label, String[] possible values)
        --%>

        <%-- Print info only if there was query for id --%>
        <c:if test="${not empty id and status ne 'deleted'}">
            <c:choose>
                <%-- found true means device was found correctly --%>
                <c:when test="${found}">
                    ID: ${id} <br />
                    <form name="changeForm" action="/devices" method="post" >
                        <input type="hidden" name="id" value="${id}">
                        <%-- Print out every piece of information about device --%>
                        <c:forEach items="${info}" var="entry">
                            ${entry.key} : ${entry.value} <br />
                        </c:forEach>
                        <%-- For radio buttons --%>
                        <c:forEach items="${radios}" var="entry">
                            ${entry.key} : 
							<c:forEach items="${entry.value}" var="option">
								<input type="radio" name="${entry.key}"
									   value="${option}" 
									   <c:if test="${option == checked[entry.key]}">
										   checked
									   </c:if>
									   > ${option} 
							</c:forEach>
							<br />
                        </c:forEach>
						<c:if test="${sessionScope.user.level >= 50}">
							<input type="submit" name="set_button" value="Set" onclick="return ActionChange();">
							<input type="submit" name="delete_button"  value="Delete" onclick="return ActionChange();">
						</c:if>


                    </form><br />
                </c:when>
                <c:otherwise>
                    Device with ID ${id} not found.
                </c:otherwise>
            </c:choose>
        </c:if>

		<c:if test="${status eq 'deleted'}">
			Device deleted.
		</c:if>

		<c:if test="${status eq 'devFail'}">
			Device has corrupted settings, modification failed.
		</c:if>

        <hr />
        Choose device:<br />
        <form name="deviceForm" action="/devices" method="post">
            <select name="id">
                <%-- ids = ids of all devices --%>
                <c:forEach items="${ids}" var="id">
                    <option value="${id}">${id}</option>"
                </c:forEach>
            </select>
            <input name="show_button" type="submit"  value="Show" onclick="return ActionDevice();">
        </form>
    </jsp:body>
</tags:page-template>
