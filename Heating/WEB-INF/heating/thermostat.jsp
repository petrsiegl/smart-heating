<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html> 
    <head>
		<title>Thermostat</title>
        <!-- <link rel="stylesheet" type="text/css" href="/resources/css/styles.css"> -->
		<link rel="stylesheet" type="text/css" href="/resources/css/heating.css">
		<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css">
        <meta charset="utf-8">
		<meta name="viewport" content="width=device-width, initial-scale=1.0">

		<script src="thermostat.js"></script> 
		
    </head>
    <body class="thermostat"  onload="init()">
		<div class="body-wrapper">

			<section class="thermostat center">
				<span id="clock"></span>

				<div id="units">
				</div>
				<div class="settings">

					Running in
					<span id="mode-setting">
						<c:choose>
							<c:when test="${sessionScope.user.level >= 50}">
								<form id="mode-form" action="/${prefix}/thermostat" method="get">
									<select id ="sel-mode" name="mode" onchange="modeChange()">
										<c:forEach items="${modes}" var="entry">
											<option value="${entry}" 
													<c:if test="${currentMode == entry}">selected</c:if>
													>${entry}</option>
										</c:forEach> 
									</select>
								</form>
							</c:when>    
							<c:otherwise>
								${currentMode}
							</c:otherwise>
						</c:choose>
					</span>
					mode.

					<div class="status">
						<c:if test ="${not empty status}">
							${status}<br />
						</c:if>
					</div>

				</div>


			</section>

			<footer class="center thermostat">
				<a class="icon" href="/${prefix}/home"><i class="fa fa-home fa-3x"></i></a>

			</footer>
		</div>
	</body> 
</html>