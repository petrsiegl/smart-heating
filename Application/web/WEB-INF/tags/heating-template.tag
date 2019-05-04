<%@tag description="Heating template" pageEncoding="UTF-8"%>
<%@taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@attribute name="head" fragment="true" %>
<tags:page-template>
	<jsp:attribute name="head">
		<jsp:invoke fragment="head"/>
    </jsp:attribute>
    <jsp:body>   

		<ul class="menu sidemenu">
			<li><a href="/${prefix}/home">Heating units</a></li>
				<c:if test="${sessionScope.user.level >= 50}">
				<li><a href="/${prefix}/createunit">Create unit</a></li>
				</c:if>
			<li><a href="/${prefix}/predefined">Predefined heating</a></li>   
		</ul>
		<jsp:doBody/>
    </jsp:body>
</tags:page-template>

