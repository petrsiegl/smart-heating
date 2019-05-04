<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<tags:page-template>
    <jsp:body>

        <a href="/smartheating/predefined.jsp">predef</a><br />

        <%-- units is map String(id),HeatingUnit   --%>
        <c:if test="${not empty units}">
            Heating units:<br />
        </c:if>
        <c:forEach items="${units}" var="entry">

            <form action="/servlet/addsmartheating" method="get">
                <input type="hidden" name="reqtype" value="change" >
                <input type="hidden" name="id" value ="${entry.key}" >
                Heating unit id: ${entry.key}<br />
                Thermometer id: ${entry.value.thermometer.ID}<br />
                Relay id: ${entry.value.relay.ID}<br /> 
                Temperature: <input type="text" name="temp" value="${entry.value.preferedTemp}">
                <input type="submit" name="set_button" value="Set">
                <input type="submit" name="delete_button"  value="Delete">
            </form>
        </c:forEach> 

        Create new heating unit:<br />
        <form action="/servlet/addsmartheating" method="get">
            <input type="hidden" name="reqtype" value="addunit" >
            <input type="text" name="id" ><br />
            <select name="idrelay">
                <c:forEach items="${relays}" var="id">
                    <option value="${id}">${id}</option>"
                </c:forEach>  
            </select><br />
            <select name="idthermo">
                <c:forEach items="${thermometers}" var="id">
                    <option value="${id}">${id}</option>"
                </c:forEach>           
            </select><br />
            <input type="submit"  value="Create">
        </form><br />

        Current Regime: ${currentMode}
        <form action="/servlet/changeregime" method="get">
            <select name="mode">
                <c:forEach items="${modes}" var="entry">
                    <option value="${entry}">${entry}</option>
                </c:forEach> 
            </select>
            <button type="submit" name="submit" value="Change">Change</button>
        </form>


    </jsp:body>
</tags:page-template>

