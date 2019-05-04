<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<tags:page-template>
    <jsp:body>
        <%--
        <script type="text/javascript">
            function showValue(newValue)
            {
                document.getElementById("range").innerHTML = newValue;
            }
        </script>
        
        <script>
            function openCity(evt, day) {
                var i, tabcontent, tablinks;
                tabcontent = document.getElementsByClassName("dayTab");
                for (i = 0; i < tabcontent.length; i++) {
                    tabcontent[i].style.display = "none";
                }
                tablinks = document.getElementsByClassName("tablinks");
                for (i = 0; i < tablinks.length; i++) {
                    tablinks[i].className = tablinks[i].className.replace(" active", "");
                }
                document.getElementById(day).style.display = "block";
                evt.currentTarget.className += " active";
            }
        </script>

        <ul class="tab">
            <li><a href="#" class="tablinks" onclick="openCity(event, 'Monday')">Monday</a></li>
            <li><a href="#" class="tablinks" onclick="openCity(event, 'Tuesday')">Tuesday</a></li>
            <li><a href="#" class="tablinks" onclick="openCity(event, 'Wednesday')">Wednesday</a></li>
            <li><a href="#" class="tablinks" onclick="openCity(event, 'Thursday')">Thursday</a></li>
            <li><a href="#" class="tablinks" onclick="openCity(event, 'Friday')">Friday</a></li>
            <li><a href="#" class="tablinks" onclick="openCity(event, 'Saturday')">Saturday</a></li>
            <li><a href="#" class="tablinks" onclick="openCity(event, 'Sunday')">Sunday</a></li>
        </ul>

        <div id="Monday" class="dayTab">
            <h2>M</h2>
        </div>

        <div id="Tuesday" class="dayTab">
            <h2>T</h2>
        </div>

        <div id="Wednesday" class="dayTab">
            <h2>W</h2>
        </div>

        <div id="Thursday" class="dayTab">
            <h2>T</h2>
        </div>
        
        <div id="Friday" class="dayTab">
            <h2>F</h2>
        </div>
        
        <div id="Saturday" class="dayTab">
            <h2>S</h2>
        </div>

        <div id="Sunday" class="dayTab">
            <h2>S</h2>
        </div>
        --%>

        <form action="/smartheating/predefined.jsp" method="get">
            <select name="day">
                <c:forEach items="${days}" var="dayEntry">
                    <option value="${dayEntry}">${dayEntry}</option>
                </c:forEach>
            </select><br />
            <select name="heatingUnitID">
                <c:forEach items="${heatingUnits}" var="hu">
                    <option value="${hu}">${hu}</option>
                </c:forEach>
            </select><br />
            <button type="submit"  value="Show">Show</button>
        </form>
        <c:if test="${not empty tableRecords}">
            Day: ${day}<br />
            Unit: ${unitID}<br />
            <table>
                <tr>
                    <th>Time</th><th>Temperature</th><th>Heating Unit ID</th>
                </tr>
                <c:forEach items="${tableRecords}" var="record">
                    <tr>
                        <td>${record.time}</td><td>${record.temperature}</td><td>${record.heatingUnitID}</td>
                    </tr>
                </c:forEach>
            </table>
        </c:if>
        <br />
        Add new setting:
        <form action="/smartheating/addPredefined">
            Time:
            <select name="addTime">
                <c:forEach items="${availibleTimes}" var="entry">
                    <option value="${entry}">${entry}</option>
                </c:forEach><br />
            </select>
            <br />
            Heating Unit:
            <select name="addUnitID">
                <c:forEach items="${heatingUnits}" var="hu">
                    <option value="${hu}">${hu}</option>
                </c:forEach><br />
            </select>
            <br />
            Day:
            <select name="addDay">
                <c:forEach items="${days}" var="dayEntry">
                    <option value="${dayEntry}">${dayEntry}</option>
                </c:forEach>
            </select><br />
            Temperature: <input name="temperature" type="text" /> <br />
            <%--
            <input type="range" value="0" min="0" max="100" step="0.5" onchange="showValue(this.value)" />
            <span id="range">0</span><br />
            --%>
            <button type="submit" value="Add">Add</button>

        </form>
    </jsp:body>
</tags:page-template>

