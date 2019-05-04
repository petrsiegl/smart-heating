<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<tags:page-template>
    <jsp:body>
        <%--    
            status: {ON,OFF} whether NAT traverser function is currently running
            address: address of the NAT traverser
            port: port of the NAT traverser
        --%>
        <h1>NAT traverser settings</h1><br />
        <form action="/traverser.jsp" method="get">
            Status: <br />
            <input type="radio"
				   name="status"
				   value="ON" 
				   <c:if test="${travStatus == 'ON'}">
					   checked
				   </c:if>
				   > ON 
            <input type="radio"
				   name="status"
				   value="OFF" 
				   <c:if test="${travStatus == 'OFF'}">
					   checked
				   </c:if>
				   > OFF<br />
            Address:<br />
            <input type="text" name="address" value="${address}"><br />
            Port:<br />
            <input type="number" name="port" value="${port}"><br />
			<%-- #future possible implementation#
			Context:<br />
            <input type="text" name="context" value="${context}"><br />
			--%>
			New password:<br />
            <input type="password" name="password" ><br />
            <input type="submit" name="save_button" value="Save">
        </form>

		<c:if test="${status eq 'fail'}">
			Modification failed.
		</c:if>

    </jsp:body>
</tags:page-template>
