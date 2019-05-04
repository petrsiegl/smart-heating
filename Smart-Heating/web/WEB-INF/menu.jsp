<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<ul class="menu">
    <li><a href="/home">Home</a></li>
		<c:if test="${sessionScope.user.level >= 50}">
		<li><a href="/create-device">Create Device</a></li>
		</c:if>
	<li><a href="/devices">Show Device</a></li>
		<c:forEach items="${processes}" var="process">
		<li><a href="/${process.key}">${process.value.name}</a></li>  
		</c:forEach>
		<c:if test="${sessionScope.user.level >= 50}">
		<li><a href="/traverser">NAT traverser</a></li>
		</c:if>
</ul>

