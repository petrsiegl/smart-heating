/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


var tempName = "temperature-preffered";
var tempBtn = "temp-btn";
var unitClass = "unit";

var unit = null;
function init() {
	startTime();
	//prepareUnitTemplate();

	loadTemps();
	setInterval(storeAndLoadTemps, 300000);
}

function prepareUnitTemplate() {



	unit = null;
}

function startTime() {
	var now = new Date();
	var h = now.getHours();
	var m = now.getMinutes();
	m = checkSingleDigit(m);
	document.getElementById('clock').innerHTML = h + ":" + m;
	var timer = setTimeout(startTime, 500);
}

function checkSingleDigit(i) {
	if (i < 10) {
		i = "0" + i;
	}  // add zero in front of numbers < 10
	return i;
}

function getUnit(id, format) {
	var xhttp;
	if (id == "" || format == "") {
		return;
	}

	xhttp = new XMLHttpRequest();
	xhttp.onreadystatechange = function () {
		if (this.readyState == 4 && this.status == 200) {
			return this.responseText;
		}
	};
	xhttp.open("GET", "unit/" + format + "/" + id + "/", true);
	xhttp.send();
}

function getUnits(format) {
	var xhttp;
	if (format == "") {
		return;
	}

	xhttp = new XMLHttpRequest();
	xhttp.onreadystatechange = function () {
		if (this.readyState == 4 && this.status == 200) {
			loadUnitsFromJson(this.responseText);
			setButtonsVisible(isModeManual());
			return;
		}
	};
	xhttp.open("GET", "unit/" + format + "/", true);
	xhttp.send();
}


function loadUnitsFromJson(jsonStr) {
	var units = JSON.parse(jsonStr);
	var container = document.getElementById("units");
	for (var i = 0; i < units.length; i++) {


		if (i == 0) {
			// Clear units
			container.innerHTML = "";
		}

		var unitDiv = document.createElement("div");
		unitDiv.className = unitClass;
		unitDiv.id = units[i].id;
		container.appendChild(unitDiv);

		var nameTag = document.createElement("div");
		nameTag.className = "nametag";
		nameTag.innerHTML = units[i].id;
		unitDiv.appendChild(nameTag);

		var unitBody = document.createElement("div");
		unitBody.className = "unit-body";
		unitDiv.appendChild(unitBody);

		var containerSpan = document.createElement("span");
		containerSpan.className = "temp-container";
		unitBody.appendChild(containerSpan);

		var minusButton = document.createElement('button');
		minusButton.className = tempBtn;
		minusButton.innerHTML = '-';
		minusButton.onclick = function () {
			addToTemperature(this, -0.5);
		};
		containerSpan.appendChild(minusButton);

		var span = document.createElement('span');
		span.className = "temp-diff " + "temperature";
		var tempDiff = (parseFloat(units[i].temperature) - parseFloat(units[i].preferredTemp)).toFixed(1);
		colorizeTemp(span, tempDiff);

		span.innerHTML = tempDiff;
		containerSpan.appendChild(span);

		span = document.createElement('span');
		span.className = tempName + " temperature";
		span.innerHTML = parseFloat(units[i].preferredTemp).toFixed(1);
		containerSpan.appendChild(span);

		var plusButton = document.createElement('button');
		plusButton.className = tempBtn;
		plusButton.innerHTML = '+';
		plusButton.onclick = function () {
			addToTemperature(this, 0.5);
		};
		containerSpan.appendChild(plusButton);
	}

}

function colorizeTemp(element, value) {
	if (value < 0) {
		element.style.color = "PaleTurquoise";
	} else if (value > 0) {
		element.style.color = "LightPink";
	} else {
		element.style.color = "lightgray";
	}
}

function storeTemps(afterCallback) {
	if (isModeManual()) {

		var units = document.getElementsByClassName(unitClass);

		var unitsArr = [];
		var unit = {};

		for (var i = 0; i < units.length; i++) {
			unit = {};
			unit["id"] = units[i].id;
			unit["preferredTemp"] = units[i].getElementsByClassName(tempName)[0].innerHTML;

			unitsArr.push(unit);
		}

		if (units.length > 0) {
			var unitsStr = JSON.stringify(unitsArr);

			var xhttp = new XMLHttpRequest();
			xhttp.onreadystatechange = function () {

				if (this.readyState == 4 && this.status == 200) {
					if (typeof afterCallback === 'function') {
						afterCallback();
					}
					return;
				}
			};

			//Can't go through this for some reason
			//xhttp.setRequestHeader("Content-Type", "application/json");
			xhttp.open("PUT", "unit/json/", true);
			xhttp.send(unitsStr);
		}


	}
}

function storeAndLoadTemps() {
	storeTemps(loadTemps);
}

function loadTemps() {
	getUnits("json");
}

function addToTemperature(sender, value) {
	var unit = sender.parentElement;
	var temp = unit.getElementsByClassName(tempName)[0];
	var diffTemp = unit.getElementsByClassName("temp-diff")[0];

	var currentTemp = +temp.innerHTML;
	if (!(currentTemp >= 99 && value > 0) &&
			!(currentTemp <= -99 && value < 0)) {
		temp.innerHTML = (currentTemp + value).toFixed(1);

		var newDiff = (+diffTemp.innerHTML - value).toFixed(1);
		diffTemp.innerHTML = newDiff;
		colorizeTemp(diffTemp, newDiff);
	}
}

function setButtonsVisible(visible) {
	var visibility = (visible == false) ? "hidden" : "visible";
	var buttons = document.getElementsByClassName(tempBtn);
	for (var i = 0; i < buttons.length; i++)
	{
		buttons[i].style.visibility = visibility;
	}
}

function getSetMode() {
	var select = document.getElementById("sel-mode");
	if (select != null) {
		return select.value;
	} else {
		return document.getElementById("mode-setting").innerHTML.trim();
	}
}

function isModeManual() {
	var value = getSetMode();
	if (value == "MANUAL") {
		return true;
	} else {
		return false;
	}
}


function modeChange() {

	setButtonsVisible(isModeManual());


	var xhttp = new XMLHttpRequest();
	xhttp.onreadystatechange = function () {
		if (this.readyState == 4) {
			if (this.status == 200) {
				return;
			} else {
				document.getElementsByClassName("status")[0].innerHTML = "Mode change failed. Mode setting on page might be inconsistent.";
			}
		}
	};
	xhttp.open("GET", "mode?mode=" + getSetMode(), true);
	xhttp.send();
}

