function isStrongPwdReg(password) {

        var regExp = /(?=.*\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%&*()]).{8,}/;

        var validPassword = regExp.test(password);

        return validPassword;

}

function isStrongPwd(password) {

     var uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

     var lowercase = "abcdefghijklmnopqrstuvwxyz";

     var digits = "0123456789";

     var splChars ="!@#$%*()";

     var o=document.getElementById("UPPERCASE");
     var ucaseFlag = contains(password, uppercase);
	 if ( o ) {
	    if ( ucaseFlag ) { o.style.color="green"; } else { o.style.color="red"; }
	 }
         o=document.getElementById("LOWERCASE");
     var lcaseFlag = contains(password, lowercase);
	 if ( o ) {
            if ( lcaseFlag ) { o.style.color="green"; } else { o.style.color="red"; }
         }
         o=document.getElementById("DIGITS");
     var digitsFlag = contains(password, digits);
	 if ( o ) {
            if ( digitsFlag ) { o.style.color="green"; } else { o.style.color="red"; }
         }
         o=document.getElementById("SPECIALS");
     var splCharsFlag = contains(password, splChars);
	 if ( o ) {
            if ( splCharsFlag ) { o.style.color="green"; } else { o.style.color="red"; }
         }
     if(password.length>=8 && ucaseFlag && lcaseFlag && digitsFlag && splCharsFlag)
           return true;
     else
           return false;

  }

function valid() {

	p1=document.getElementByID("p1");
	p2=document.getElementByID("p2");

	if ( p1 && p2 && p1.value === p2.value && isStrongPwd(p1.value) ) { return true; }
	
	return false;
}

function checkValid() {
	return valid();
}

function test(){ alert("test include"); }

function pickupUser() { 
	if ( user === "" ) { window.alert("Login first"); return; }
	pickupUser(user); 
}
function pickupUser(uu) {
	if ( uu === "" ) { window.alert("unknown user error"); return; }
	sref=ref;
        ref="userknowledge.html";
        obj="";
	loadContext("Edit.jsp","user="+uu+"&auth="+auth+"&action=pickupUser", obj);
	ref=sref;
}

function validate(evt,obj) {
  var theEvent = evt || window.event;

  // Handle paste
  if (theEvent.type === 'paste') {
      key = event.clipboardData.getData('text/plain');
  } else {
  // Handle key press
      var key = theEvent.keyCode || theEvent.which;
      key = String.fromCharCode(key);
  }
  var b=false;
  var regex = /[0-9]|\./;
  if( !regex.test(key) ) {
	  var fname = obj.value;
	  if ( key !== "-"  || ( fname.length >= 1 && key === "-" )  ) {
	  	b=true;
	  }
  }
  if ( b) {
    theEvent.returnValue = false;
    if(theEvent.preventDefault) theEvent.preventDefault();
  }
}

function loadNextPage(next){
	if ( next > 0 ) { page++; }
	else { page--; 
		if ( page < 0 ) { page=0; }
	}
	loader("View.jsp?oscar","user="+user+"&auth="+auth+"&oscar=map&minpage="+page+"&page=20", ""); 
}

function mynewval(tab,id,obj) {
	//alert(tab+":"+id+":"+obj);
	v="";
	m=document.getElementById("pmanu"+id);
	if ( m ) {
		v="&manu="+m.options[m.selectedIndex].value;
		v=v+"&manuname="+m.options[m.selectedIndex].innerHTML;
	}
	m=document.getElementById("pname"+id);
	if ( m ) {
		v=v+"&prodname="+m.innerHTML;
	}
	loadContext("Edit.jsp","user="+user+"&auth="+auth+"&action=oscarprod&tab="+tab+"&mid="+id+"&val="+obj.value+v, "");
}

function updateManuProdList(tab) {
    var m = document.getElementById("manufactors");
	if ( m ) {
	   var t = document.getElementById(tab);
	   if ( t ) {
		   for (var i = 0, row; row = t.rows[i]; i++) {
			   nam=t.rows[i].cells[0].innerHTML;
			    id=t.rows[i].cells[1].innerHTML;
			   sel=t.rows[i].cells[3].getElementsByTagName("select")[0];
			logger(3,"row["+i+"]:"+nam+":"+id+":"+sel);
			   for(var j=0; j < m.options.length; j++) {
				//var opt = m.options[j];
				var opt = document.createElement('option');
    					opt.value = m.options[j].value;
    					opt.innerHTML = m.options[j].innerHTML;
				if ( ! m.options[j].innerHTML.startsWith("[") ) { 
					sel.appendChild(opt);
				}
  			   }
		   }
	   } else {
		   logger(4,"tabelle "+tab+"not found");
	   }
	} else {
		logger(4,"manufactors not found");
	}
}

function getProd(obj,id){
	loader("View.jsp", "action=getProducts&from="+obj.options[obj.selectedIndex].value+"&for=pprod"+id+"&user="+user+"&auth="+auth, "");
}

function updateAdmin(obj,u) {
  if ( obj ) {
	  loader("Edit.jsp", "action=updateAdmin&"+obj.id+"="+obj.checked+"&impuser="+u+"&user="+user+"&auth="+auth, "");
  }
}

function getProdVal(obj,id){
	var m = document.getElementById(id);
	if ( m ) {
		m.value = obj.options[obj.selectedIndex].value;
		var event = new Event('change');
		m.dispatchEvent(event);
	}
}

