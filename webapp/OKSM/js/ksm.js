var loaded=0;
var forceEncode=0;
var rkey="";
var manucount=0;
var prodcount=0;

function loader(url, data, callcb){
    var nhttp = new XMLHttpRequest();
    nhttp.onreadystatechange = function () {
            if (this.readyState === 4 ) {
                if ( nhttp.responseText !==  "" ) {
		    if (forceEncode === 0 ) { 
                    	mana(nhttp.responseText);
		    } else {
			mana( Base64.decode(nhttp.responseText) );
		    }
                }
            }
    };
    nhttp.open("POST",url, true);
    log(2, "X-ENC:"+forceEncode);
    nhttp.setRequestHeader("X-ENC", forceEncode);
    nhttp.setRequestHeader("X-RDM", rkey );
    var r=getIndexRef();
    if ( r != "" ) {
        //console.log(r);
        nhttp.setRequestHeader('X-Referer', r);
    }

    if ( forceEncode === 1 ) {
	    data = Base64.encode(data);
    }
    log(2, "data:"+data);
    nhttp.send(  data );
}

function mana(str) {
  if ( str && str != "" ) {
    eval(str);
  }
}

function getIndexRef() {
	if ( ref !== "" ) { return ref; }
	return window.top["ref"];
}

var debug=0;
var ref="";
function loadContext(url, data, obj) {
    var nhttp = new XMLHttpRequest();
    nhttp.onreadystatechange = function () {
            if (this.readyState === 4 ) {
		  var t = document.getElementById(obj);
		  if ( t ) {
			d=this.responseText;
			if ( forceEncode === 1 ) { data = Base64.encode(d); }
                    	t.innerHTML=d;
			var myScripts = t.getElementsByTagName("script");
    			if (myScripts.length > 0) {
			   for( i=0; i<myScripts.length; i++ ) {
        			eval(myScripts[i].innerHTML);
			   }
    			}
		  }
            }
    };
    nhttp.open("POST",url, true);
    nhttp.setRequestHeader("X-ENC", forceEncode);
    nhttp.setRequestHeader("X-RDM", rkey );
    var r = getIndexRef();
    if ( r != "" ) {
	//console.log(ref);
        nhttp.setRequestHeader('X-Referer', r);
    }
    nhttp.send(data);
}


function show(obj, val) {
	target=document.getElementById(obj);
        if ( target ) {
		t='none';
		if (  val === "true" || val === "1" ) {
			t='inline';	
		}
		target.style.display=t;
	}
}

function checkOK(obj)
{
        if ( obj.checked ) {
	   val = obj.name.substr(0, obj.name.length-1 );
	   target=document.getElementById(val);
	   if ( target ) {
	      target.checked = true;
	      val = "manu_"+val.substr(5, val.length-5 );
	      target=document.getElementById(val);
	      if ( target && target.checked ) {
		      return true;
	      } else {
		      alert("enable Cloud on Manufactor first ");
		      return false;
	      }
	   }
	}
	return true;
}

function logger(level, msg ){ if ( level <= debug ) { console.log(msg); } }
function log(msg) { logger(4,msg); }

function wait4SelectCount(id, count) {
	wait4LoadedId(id);
	maxwait=5000;
        waits=0;	
	doc=document.getElementById(id);
	if ( doc ) {
		while ( waits < maxwait ) {
			if ( doc.children('option').length >= count ) {  waits += maxwait ; } else {  wait(300); waits +=300; }
		}
	}
}

function wait4LoadedId(id) {
	if ( id  ) {
		maxwait=5000;
		waits=0;
                while ( waits < maxwait ) {
			if ( document.getElementById(id) !== null ) { waits += maxwait ; } else {  wait(300); waits +=300; }
		}
	}
}

function wait4(namObj,val){
	var key = window[ namObj ]; 
	if ( key ) {
		maxwait=5000;
                waits=0;
                while ( waits < maxwait ) {
			if ( key !== val ) { waits += maxwait ; } else {  wait(300); waits +=300; }
		}
	}
}

function wait(time) {
	setTimeout(() => {  }, time);
}

function wait4selectOption(obj, query){
	if ( obj ) {
		log("start wait:"+obj);
		maxwait=8000;
		waits=0;
		while ( waits < maxwait ) { 
		       target=document.getElementById(obj);
		       if ( target && target.options.length > 1 ) {
			       log("found after :"+waits+"  ->"+target.options.length);
			       waits += maxwait ;
			} else {
			       wait(300);
			       waits +=300;
				if ( target ) {
					log(obj+" length:"+target.options.length);
				}
			       log("wait:"+obj+" ->"+waits);
			}
		}
		log("fin wait:"+obj+" ("+waits+")");
		b=optionClick; 
		optionClick=false;
		  selectOption(obj, query);
		optionClick=b;
		log("fin select:"+obj);
	} else {
		log("no obj:"+obj);
	}
}

function resetValue(){
	obj=document.getElementById("csRegio");
	if ( obj ) { obj.selectedIndex = 0; }
	obj=document.getElementById("manufactors");
        if ( obj ) { obj.selectedIndex = 0; }
	obj=document.getElementById("products");
        if ( obj ) { obj.selectedIndex = 0; }
}

function lookupValue(cat){
	var post="action=getSolution&window=ResultDiv&cat="+cat;
	obj=document.getElementById("csRegio");
	if ( obj ) {  post += "&"+obj.id+"="+Base64.encode( obj.options[ obj.selectedIndex].value ); }
	obj=document.getElementById("manufactors");
        if ( obj ) {  post += "&"+obj.id+"="+Base64.encode( obj.options[ obj.selectedIndex].value ); }
	obj=document.getElementById("products");
        if ( obj ) {  post += "&"+obj.id+"="+Base64.encode( obj.options[ obj.selectedIndex].value ); }
	obj=document.getElementById("soltext");
        if ( obj ) {  post += "&"+obj.id+"="+Base64.encode( obj.value ); }
	if ( user !== "" ) {
		      post += "&user="+Base64.encode( user );
		      post += "&auth="+auth;
	}
	loader("View.jsp", post, "ResultDiv" );
}

function click(txt) {
        var l=document.getElementById(txt);
	logger(0,"found : "+l);
        if ( l ) {
                l.click();
        }
}

function clickOnSelect(sel, val, max) {
	 if ( sel ) {
		 logger(3, "clickOnSelect - "+sel.id+" found");

		 j=wait4SelectOptions(sel,max);
		 while ( j === -1 ) {
			j=wait4SelectOptions(sel,max);
		 }
		 j=sel.options.length;
		 logger(4, "clickOnSelect - "+sel.id+" len:"+j);
		 for( i=0; i<j; i++ ) {
			 if ( sel.options[i].value === val || sel.options[i].innerHTML === val ) {
				logger(2, "clickOnSelect - "+sel.id+" value:"+val+" found - send click");
				sel.value=sel.options[i].value;
				sel.selected=true;
				if ( sel.onchange != "" ) {
					var s= sel.onchange.toString();
					var ev = s.substring( s.indexOf("{")+1, s.lastIndexOf("}") ).replace( "this", "sel" );	
					//alert(ev);
					eval(ev);
				}
				sel.options[i].click();
				i=j;
			 } else {
				logger(3, "clickOnSelect - "+sel.id+" value:"+val+" not found ->i:"+i+":"+sel.options[i].value+":");
			 }
		 }
	 } else {
		 logger(4, "clickOnSelect - "+sel+" not found");
	 }
}

function wait4SelectOptions(sel, comp) {
	if ( sel ) {
		if ( ! comp || ! Number.isInteger(comp) ) { comp=0; }
		j=sel.options.length;
	        var loop=100;
		var a=0;
		var max=2000;
                while ( a<max && j< comp ) {
                        j=sel.options.length;
			a += loop;
                        wait(loop);
                }
		return sel.options.length;	
	}
	return -1; 
}

function removeAllOption(sel,c) {
  if ( sel ) {
   var i, L = sel.options.length - 1;
   for(i = L; i >= c; i--) {
      sel.remove(i);
   }
  }
}

function removeExtraOptions(sel) { removeAllOption(sel,1); }
function removeAllOptions(sel)   { removeAllOption(sel,0); }

function ImgError(source){
            empty1x1png = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVQI12NgYAAAAAMAASDVlMcAAAAASUVORK5CYII=";
            source.src = "data:image/png;base64," + empty1x1png;
            source.onerror = "";
            return true;
}

var optionClick=true;

function selectOption(obj, query ) {
	val=query.split("&")[0];
	target=document.getElementById(obj);
	log("selectOption |"+obj+"||"+val+"|"+target+"|");
	if ( target &&  val !== '' ) {
		log("selectOption length:"+target.options.length);
		for ( j=0; j<= target.options.length; j++ ) {
			t=""; v="";
			if (  target.options[j] && target.options[j].innerHTML ) { t=target.options[j].innerHTML; }
			if (  target.options[j] && target.options[j].value     ) { v=target.options[j].value;	  }
			logger(3,"selectOption verify t|"+t+"| v|"+v+"| with |"+val+"|");
			if ( t === val || v === val ) {
				log("selectOption change |"+obj+"||"+val+"|");
				target.selectedIndex = j;
				if (! optionClick || optionClick ) {
					target.dispatchEvent(new Event('change', { bubbles: true }));
				}
                                break;
			}
		}
	}
}
function updateSelect(obj, target, act) {
	val=obj.options[obj.selectedIndex].value;
	if ( document.getElementById("manu_name") ) {
		val = val+"&manu="+document.getElementById("manu_name").value;
	}
	loader("View.jsp","action=updateSelect&"+target+"="+val+"&act="+act+"&"+window.location.href.slice(window.location.href.indexOf('?') + 1), mana);
}
function updateSelectVal(obj, target) {
	if ( obj ) {
		val=obj.options[obj.selectedIndex].value;
		loader("View.jsp","action="+act+"&select="+val+"&target="+target, mana);
	}
}

function getSelected(target,want) {
	ret="";
	obj =  document.getElementById(target);
	if ( obj ) {
	     switch( want ) {
		     case 'value':    
					ret=obj.options[obj.selectedIndex].value;
			     		break;
		     case 'text' :	
			     		ret=obj.options[obj.selectedIndex].text;
			     		break;
	     }
	}
	return ret;
}



function updateEnables(obj,val) {
	target=document.getElementById(obj);
	if ( target ) {
		t=true;
                if ( val == '1' || val == 'true' ) { t=false; }
		target.disabled=t;
	}
}
function updateEnable(obj) { updateEnables(obj, '1'); }

function updateInner(obj, txt) {
	target=document.getElementById(obj);
	if ( target ) { target.innerHTML=txt; }
}

function updateInput(obj,txt) {
	target=document.getElementById(obj);
        if ( target ) { target.value=txt; }
}
function updateInputReadOnly(obj) {
        target=document.getElementById(obj);
        if ( target ) { target.readOnly = true; }
}
function updateInputWriteable(obj) {
        target=document.getElementById(obj);
        if ( target ) { target.readOnly = false; }
}

function updateCheckbox(obj, val) {
	 target=document.getElementById(obj);
	 if ( target ) { 
		 t=false;
		 if ( val == '1' || val == 'true' ) { t=true; }
		 target.checked = t;
	 }	 
}
function updateButton(obj, val) {
	 if ( updateButtonName(obj, val) ) {
		 updateEnable(obj);
	 }
}
function updateButtonName(obj, val) { 
	target=document.getElementById(obj);
        if ( target ) {
             target.value=val;
	     return true;
	}
	return false;
}

function updateButtonIf(obj, val, obj1, val1 ) {
    target = document.getElementById(obj); 
    if ( target ) {
	if ( target.nodeName === 'button' ) {
		if ( target.value === val1 ) {
			updateButton(obj1, val1);	
		}
	} else {
	    if ( target.nodeName === 'select' ) { 
		 if( target.options[target.selectedIndex].value === val ) {
			 updateButton(obj1, val1);
		 } 
	    }
	}
    }
}

function updateButtonToggle(obj, val, val1 ) {
	 target = document.getElementById(obj);
	 if ( target ) {
		 if ( target.value === val ) { target.value === val1; } else { target.value === val; }
	 }
}

function removeOptionFromSelect(obj, val) {
	target = document.getElementById(obj);
	if ( target ) { 
		for (var i=0; i<target.length; i++) {
    			if (target.options[i].value === val ) {
        			target.remove(i);
			}
		}
	}
}

var user="";
var name="";
var enc="";
var auth="";

function impersonate(obj, from) {
    if ( ! obj.disabled ) {
	target = document.getElementById(from);
	val=target.options[target.selectedIndex].value;
        loader("Log.jsp","action="+from+"&"+from+"="+val+"&user="+user+"&auth="+auth, mana);
    }
}

function login() {
	//var check=confirm("Login");
	sref=ref;
	ref="login.html";
	obj="loginDiv";
	loadContext("Page.jsp","page=login&auth="+auth+"&", obj);
	ref=sref;
	target = document.getElementById(obj);
	if ( target) target.style.display='';
}

function updateUser() {
	u=document.getElementById("login");
	if ( u )  {
		ar=u.innerHTML.split(")");
		ar = ar[0].split("(");
		if ( ar[1].length === 3 ) {
			user=ar[1]; name=ar[0];
		} else { user=""; name="";}
	} else { user=""; name=""; }
}

function sendauth(btn) {
    //alert(btn.value);
    if ( btn.value === "Login" ) {
	 u=document.getElementById("fname").value;
	 p=document.getElementById("pname").value;
	 loader("Log.jsp","action=login&auth="+auth+"&token1="+u+"&token2="+p, mana);
    } 
	obj="loginDiv";
	target = document.getElementById(obj);
        if (target) target.style.display='none';
	window.location.reload();    
}

function logout(){
	document.execCommand('ClearAuthenticationCache');
	loader("Log.jsp","action=logoff&auth="+auth, mana);
	window.location.reload();
}

function showdiv(id){
        target = document.getElementById(id);
	if ( target )  {
		target.style.display = (target.style.display=='block'?'none':'block');
	}
}

var communication=true;

function openTab(evt, tabName, clName, tablink) {
	var sdeb=debug;
	debug=4;
	f="openTab";
	logger(4, f+" starting ");
	tabcontent = document.getElementsByClassName(clName);
	if ( tabcontent ) {
		logger(1, f+" close "+clName+" tabs ["+tabcontent.length+"]");
		for (i = 0; i < tabcontent.length; i++) {
			logger(3, "close tabs - "+tabcontent[i].id);
    			tabcontent[i].style.display = "none";
  		}
	} else {
		 logger(3, f+" clName:"+clName+" not found");
	}

	tablinks = document.getElementsByClassName(tablink);
	if ( tablinks ) {
		for (i = 0; i < tablinks.length; i++) {
			logger(3, f+" close tablinks - "+tablinks[i].id);
    			tablinks[i].className = tablinks[i].className.replace(" active", "");
  		}
	}
	logger(3, f+" show tabnam - "+tabName);
	obj=top.document.getElementById(tabName);
	obj.style.display = "block";
        if ( evt) { evt.currentTarget.className += " active"; }
	call(obj,'sub');
	//openT(event, tabName,'tabcontent','tablinks');
	if ( obj.innerHTML == "" ) {
	  if ( communication )  {
		loaded++;
		loadContext("Page.jsp", "page="+tabName+"&auth="+auth, tabName);
        	loader("./View.jsp?selector","","impuser");
	  }
	}

        var x = (document.querySelector('#body_layer').offsetHeight - 750) + 150 ;
	var o = document.getElementById("spacerID");
	if ( o ) { o.style.height = ""+x+"px"; }

	logger(3, f+" Tab:"+tabName);
	if ( tabName === "PickUp" ) {
		logger(3, f+" like to clear Create Tab");
		o=document.getElementById("Create");
		if ( o ) { o.innerHTML=""; logger(3, f+" cleared Create Tab");}
		o=document.getElementById("SelSolutions");
		if ( o ) { o.selectedIndex = 0; }
	}
	logger(4, f+" stopped");
	debug=sdeb;
}

function call_sub(obj) {
        if ( lastsub  !== null ) {
                 var li=document.getElementById("l"+lastsub.id.substr(1) );
                 if ( li ) {
                        li.setAttribute("style", "background-color:#6A6A6A;");
                 }
        }
        if ( obj ) {
                var li=document.getElementById("l"+obj.id.substr(1) );
                if ( li ) {
                        li.setAttribute("style", "background-color:#abcA6A;");
                }
                lastsub=obj;
        }
}

function call(obj,inf) {
        us=window.top["user"];
        au=window.top["auth"];
        if ( inf === "sub" ) {
                call_sub(obj);
                return;
        }
        if ( lastnav !== null ) {
                 var li=document.getElementById("l"+lastnav.id.substr(1) );
                if ( li ) {
                        li.setAttribute("style", "background-color:#6A6A6A;");
                        showUp( document.getElementById("s"+lastnav.id.substr(1) ) ) ;
                }

        }
        if ( obj ) {
                var li=document.getElementById("l"+obj.id.substr(1) );
                if ( li ) {
                        li.setAttribute("style", "background-color:#abcA6A;");
                        showUp( document.getElementById("s"+obj.id.substr(1) ) );
                }
                var sp = obj.href.split("#");
                window.top["ref"]=sp[ sp.length - 1 ]+".html";
                window.top["loadContext"]("Page.jsp", "page="+sp[ sp.length - 1 ]+"&user="+us+"&auth="+au, "inner");
                lastnav=obj;
        }
}

function openT(ev,like,tabc, tablinks ) {
         window.top["openTab"](ev, like,tabc,tablinks);
}

var lastnav=null;
var lastsub=null;

function updateTab(obj) {
    if (obj) {
	    tr=obj.parentElement.parentElement;
           tab=tr.parentElement;
	if ( obj.value === "+" ) {
		oldStr = tr.innerHTML.split("\"")[3];
		oldId  = oldStr.substr("manufactors".length);
		newId  = tab.rows.length+1; 
		newStr = "manufactors"+newId;
		alert("add ||"+oldId+"<||"+newId+"||");
		row = tab.insertRow();
		row.innerHTML = tr.innerHTML.replaceAll(oldStr,newStr);
	}else{
	    if ( tab.rows.length > 3 ) {
		alert("remove"+tab.rows.length);
		    tr.remove();
	    }
	}
    }
}
function trremove(obj, n) {
	if ( obj ) {
	   tr=obj.parentElement.parentElement;
	   tab=tr.parentElement;
	   if ( tab.rows.length > 1 ) {
	   	tr.remove(); updateSol++;
	   }
	}
}

var updateSol=0;

function tradd(obj) {
	if( obj ) {
	    tab=obj.parentElement.parentElement.parentElement;	
	    if ( tab.nodeName === "TBODY" ) { tab=tab.parentElement; }
	    logger(3,tab.nodeName+"->"+tab.id);
	    nrowt = tab.insertRow();
	    n=tab.rows.length - 1 ;
	    cell = nrowt.insertCell();
		 cell.innerHTML=""+n+"";
	    cell = nrowt.insertCell();
		 cell.innerHTML="<button onclick=\"trremove(this,'"+n+"');\" id=\""+tab.id+"minus"+n+"\">-</button>";
	    switch( tab.id ) {
	        case 'offers'   : {
					cell = nrowt.insertCell();
					     b ="<select id='"+tab.id+"Typ"+n+"' >";
                                             b += "<option value='Workshop' >Workshop</option>\n";
                                             b += "<option value='POC' >POC</option>\n";
                                             cell.innerHTML=b+"</select>";

                                        cell = nrowt.insertCell();
                                              cell.innerHTML="<input type=text id='"+tab.id+"Value"+n+"' style=\"width: 97%;\" value=\"\"/>";
					cell = nrowt.insertCell();
                                              cell.innerHTML="<input type=text id='"+tab.id+"PT"+n+"' style=\"width: 97%;\" onkeypress='return event.charCode >= 48 && event.charCode <= 57' value=\"\" /><input type=hidden id='"+tab.id+"ID"+n+"' value=\"0\" />";
                                        break;
				  }
	        case 'feature'  : 
		case 'benefits' : {	
					cell = nrowt.insertCell();
					     b="<select id='"+tab.id+"Typ"+n+"' >";
				             if ( tab.id === 'feature' ) { b += "<option value='Feature' >Feature</option>\n"; }
					      else {
						      b += "<option value='Vorteil' >Vorteil</option>\n"; 
						      b += "<option value='Nachteil' >Nachteil</option>\n";
					      }
					     cell.innerHTML=b+"</select>";
					cell = nrowt.insertCell();
					      cell.innerHTML="<input type=text id='"+tab.id+"Value"+n+"' style=\"width: 97%;\" value=\"\"/><input type=hidden id='"+tab.id+"ID"+n+"' value=\"0\" />";
					break;
				  }
		case 'branches' : {
					cell = nrowt.insertCell();
					cell.innerHTML="<select id='"+tab.id+"Typ"+n+"'  >\n<option value='disable' >Disabled</option>\n<option value='enable' >Enabled</option>\n</select>";
					//cell = nrowt.insertCell();
					//cell.innerHTML=" ";
					cell = nrowt.insertCell();
					cell.innerHTML="<input type=text id='"+tab.id+"Value"+n+"' style=\"width: 97%;\" value=\"0\"/>";
					break;
		    		  }
		case 'manuprod' : {
			cell = nrowt.insertCell();
			cell.id = tab.id+"ID+"+n;
                        //cell.innerHTML="manufactor";
                        manu = document.getElementById("manufactors");

                        cell.innerHTML="<select id='manu"+n+"' onChange=\"updateProds(this, '"+n+"');\" > </select>";
                        select = document.getElementById("manu"+n);
                        for ( i=0; i<manu.length; i++  ) {
                                var opt = document.createElement('option');
                                opt.value=manu.options[i].value;
                                opt.innerHTML=manu.options[i].innerHTML;
                                select.appendChild(opt);
                        }

                        cell = nrowt.insertCell();
                                cell.innerHTML="<select id='prods"+n+"' onChange=\"updateSol++;\" > </select>";
                        cell = nrowt.insertCell();
                                cell.innerHTML="<select id='level"+n+"' onChange=\"updateSol++;\" > </select>";
                        break;

	        }
	        defaults : {
	    		cell = nrowt.insertCell();
		 	//cell.innerHTML="manufactor";
		 	manu = document.getElementById("manufactors"); 
	   	 	
		 	cell.innerHTML="<select id='manu"+n+"' onChange=\"updateProds(this, '"+n+"');\" > </select>";
		 	select = document.getElementById("manu"+n);
		 	for ( i=0; i<manu.length; i++  ) {
				var opt = document.createElement('option');
                            	opt.value=manu.options[i].value;
                            	opt.innerHTML=manu.options[i].innerHTML;
                        	select.appendChild(opt);
		 	}
		 
	    		cell = nrowt.insertCell();
		 		cell.innerHTML="<select id='prods"+n+"' onChange=\"updateSol++;\" > </select>";
	    		cell = nrowt.insertCell();
		 		cell.innerHTML="<select id='level"+n+"' onChange=\"updateSol++;\" > </select>";
			break;
		}
	    }
	    cell = nrowt.insertCell();
		 cell.innerHTML="<button onclick=\"tradd(this);\" id=\""+tab.id+"plus"+n+"\">+</button>";

	}

}

function pickupSolution(obj) {
	if ( obj ) {
	     pickupSol=true;
	     document.getElementById("createln").click();
	     o=obj;
	     if ( obj.options ) { o=obj.options[obj.selectedIndex].value; }
	     loader("View.jsp", "action=pickupSolution&id="+o+"&user="+user+"&auth="+auth,"");
	}
}
function createSolution(){
	target = document.getElementById("csName");
	if ( target && target.value !== "" ) {
	   var post="action=setSolution&auth="+auth;
	   var arr = ["csName", "csId","csPT" ,"csShortDesc", "csLongDesc"];
	   for ( i=0; i<arr.length; i++ ) {

		   logger(1,"pick value for:"+arr[i]+":");
		   target = document.getElementById(arr[i]);
		   if ( target ) {
		        post += "&"+arr[i]+"="+Base64.encode(target.value);
		   }
	   }
	   arr =  ["benefits", "feature", "offers", "manuprod", "branches", "anchors" ];
	   for ( i=0; i<arr.length; i++ ) {
		tab = document.getElementById(arr[i]);
		if ( tab ) {
			logger(0, "table obj:"+arr[i]+": found");
			var trs = tab.getElementsByTagName("tr");
			for (var j=1; j<trs.length; j++) {
				iname=arr[i]+"ID"+j;
				logger(0, "check iobj:"+iname+":");
				iobj= document.getElementById(iname);
				val="";
				if ( iobj ) {
				     logger(0, "iobj:"+iname+" found");
                                        if ( iobj.value ) { val= iobj.value; }
                                         else { 
						 j=0;
						 if ( Number.isInteger(iobj.selectedIndex) ) { j=iobj.selectedIndex; }
						 logger(0, "iobj:"+iname+"  selIndex:"+j);
						 val = iobj.options[j].value;  
					 }
                                        logger(0, "iobj:"+iname+" found with value:"+val);
                                         post += "&"+arr[i]+"-id"+j+"="+Base64.encode(val);


					iobj= document.getElementById( iname=arr[i]+"Typ"+j ) ;
					if ( iobj ) { post += "&"+arr[i]+"-typ"+j+"="+Base64.encode( iobj.options[iobj.selectedIndex].value ); }

					iobj= document.getElementById( iname=arr[i]+"Value"+j ) ;
					if ( iobj ) { post += "&"+arr[i]+"-Value"+j+"="+Base64.encode( iobj.value ); }

					iobj= document.getElementById( iname=arr[i]+"PT"+j ) ;
					if ( iobj ) { post += "&"+arr[i]+"-PT"+j+"="+Base64.encode( iobj.value ); }
				    
				} else {
				    logger(0, "check iobj:"+iname+": not found - check extras");
				    if ( arr[i] === "manuprod" ) { 
					     iobj= document.getElementById( iname="manu"+j ) ;
                                             if ( iobj ) { post += "&prod-Manu"+j+"="+Base64.encode( iobj.options[iobj.selectedIndex].value ); }

                                            iobj= document.getElementById( iname="prods"+j ) ;
                                             if ( iobj ) { post += "&prod-Prod"+j+"="+Base64.encode( iobj.options[iobj.selectedIndex].value ); }

                                            iobj= document.getElementById( iname="level"+j ) ;
                                             if ( iobj ) { post += "&prod-Level"+j+"="+Base64.encode( iobj.options[iobj.selectedIndex].value ); }

				    }
				}

				logger(0, iname+" -> tr["+j+"] loop done");

			}
		}
	   }

	   logger(1, "post create Solution:"+post); 
	   loader("Edit.jsp", post,"cshandle");

	}else{
	   alert("Solution Error - Name not defined");
	}
}

var pickupSol=false;
function updateProds(obj, num){
	//alert(obj.options[obj.selectedIndex].value+"-->"+num);
	loader("View.jsp", "action=getProducts&manuid="+obj.options[obj.selectedIndex].value+"&window=prods"+num+"&levelid=level"+num+"&auth="+auth,"prods"+num);
	 pickupSol=false;
}

function setValue(obj,val) {
	logger(3, "like to set "+val+" on obj:"+obj);
	target = document.getElementById(obj);
        if ( target ) {  
	        logger(2, "update "+obj+" with value :"+val+":");	
		target.value=val; 
	}
}
/**
*
*  Base64 encode / decode
*  http://www.webtoolkit.info/
*
**/
var Base64 = {

    // private property
    _keyStr : "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=",

    // public method for encoding
    encode : function (input) {
        var output = "";
        var chr1, chr2, chr3, enc1, enc2, enc3, enc4;
        var i = 0;

        input = Base64._utf8_encode(input);

        while (i < input.length) {

            chr1 = input.charCodeAt(i++);
            chr2 = input.charCodeAt(i++);
            chr3 = input.charCodeAt(i++);

            enc1 = chr1 >> 2;
            enc2 = ((chr1 & 3) << 4) | (chr2 >> 4);
            enc3 = ((chr2 & 15) << 2) | (chr3 >> 6);
            enc4 = chr3 & 63;

            if (isNaN(chr2)) {
                enc3 = enc4 = 64;
            } else if (isNaN(chr3)) {
                enc4 = 64;
            }

            output = output +
            this._keyStr.charAt(enc1) + this._keyStr.charAt(enc2) +
            this._keyStr.charAt(enc3) + this._keyStr.charAt(enc4);
        }
        return output;
    },

    // public method for decoding
    decode : function (input) {
        var output = "";
        var chr1, chr2, chr3;
        var enc1, enc2, enc3, enc4;
        var i = 0;

        input = input.replace(/[^A-Za-z0-9\+\/\=]/g, "");

        while (i < input.length) {

            enc1 = this._keyStr.indexOf(input.charAt(i++));
            enc2 = this._keyStr.indexOf(input.charAt(i++));
            enc3 = this._keyStr.indexOf(input.charAt(i++));
            enc4 = this._keyStr.indexOf(input.charAt(i++));

            chr1 = (enc1 << 2) | (enc2 >> 4);
            chr2 = ((enc2 & 15) << 4) | (enc3 >> 2);
            chr3 = ((enc3 & 3) << 6) | enc4;

            output = output + String.fromCharCode(chr1);

            if (enc3 != 64) {
                output = output + String.fromCharCode(chr2);
            }
            if (enc4 != 64) {
                output = output + String.fromCharCode(chr3);
            }
        }

        output = Base64._utf8_decode(output);

        return output;
    },

    // private method for UTF-8 encoding
    _utf8_encode : function (string) {
        string = string.replace(/\r\n/g,"\n");
        var utftext = "";

        for (var n = 0; n < string.length; n++) {

            var c = string.charCodeAt(n);

            if (c < 128) {
                utftext += String.fromCharCode(c);
            }
            else if((c > 127) && (c < 2048)) {
                utftext += String.fromCharCode((c >> 6) | 192);
                utftext += String.fromCharCode((c & 63) | 128);
            }
            else {
                utftext += String.fromCharCode((c >> 12) | 224);
                utftext += String.fromCharCode(((c >> 6) & 63) | 128);
                utftext += String.fromCharCode((c & 63) | 128);
            }
        }
        return utftext;
    },

    // private method for UTF-8 decoding
    _utf8_decode : function (utftext) {
        var string = "";
        var i = 0;
        var c = c1 = c2 = 0;

        while ( i < utftext.length ) {

            c = utftext.charCodeAt(i);

            if (c < 128) {
                string += String.fromCharCode(c);
                i++;
            }
            else if((c > 191) && (c < 224)) {
                c2 = utftext.charCodeAt(i+1);
                string += String.fromCharCode(((c & 31) << 6) | (c2 & 63));
                i += 2;
            }
            else {
                c2 = utftext.charCodeAt(i+1);
                c3 = utftext.charCodeAt(i+2);
                string += String.fromCharCode(((c & 15) << 12) | ((c2 & 63) << 6) | (c3 & 63));
                i += 3;
            }
        }
        return string;
    }
}

function addtr(tab, html) {
	var tableRef = document.getElementById(tab);
    if ( tableRef ) {
	    logger(2,"addtr - table of :"+tab+": found");
	var tbodyRef = tableRef.getElementsByTagName('tbody')[0];
	var newRow = tbodyRef.insertRow(tableRef.rows.length);
	    newRow.innerHTML = html;
	    logger(2,"add to table :"+tab+": ->|"+html+"|<-");
    } else {
	    logger(3,"addtr - no table found of :"+tab+":");
    }
}

function delalltr(tab) {
	 delalltr(tab,0);
}
function delalltr(tab,j) {
	var tableRef = document.getElementById(tab);
    if ( tableRef ) {
            x = tableRef.getElementsByTagName("tr").length;
           for ( i=x; i>j; i-- ) {
                   deltr(tab, i-1);
           }
    }

}
function deltr(tab, line) {
    var tableRef = document.getElementById(tab);
    if ( tableRef ) {
	    tableRef.deleteRow(line);
    }

}

function updateKM(o) {
	loader('Edit.jsp', "action=updateKM&id="+Base64.encode(o.id)+"&val="+Base64.encode((o.checked === true)?"1":"0")+"&obj="+Base64.encode(o.name)+"&user="+user+"&auth="+auth, '');
}

function removeOptions(obj,l) {
    o=document.getElementById(obj);
    if ( o && o.tagName === 'SELECT' ) {
	    max = o.options.length - 1;
   	    for(i = max; i >= 0, i>=l; i--) {
      		o.remove(i);
   	    }
    }
}

function transOptions(from, to ) {
    o=document.getElementById(from); 
    t=document.getElementById(to);
    logger(0,"find element o:"+o+": t:"+t+": ");
    if ( o && t && o.tagName === 'SELECT' && t.tagName === 'SELECT') {
	    logger(0,'update '+to);
	    removeOptions(to,1);
	    max = o.options.length;
	    logger(0,'max '+max);
	    for(i = 0 ; i < max; i++) {
               	t.appendChild( o.options[i] );
            }

    } else {
	    logger(0,"skip find element o:"+o+": t:"+t+": ");
    }
}

function addScript(src) {
	if ( src && src !== "" ) {
	  var l = document.getElementById(src);
	  if ( l ) {
	   	logger(0,"add script : "+src);
		var sc = document.createElement('script');
		    sc.src=src;
		    sc.id=src;
		
		var nhttp = new XMLHttpRequest();
    		nhttp.onreadystatechange = function () {
            	  if (this.readyState === 4 ) {
                	if ( nhttp.responseText !==  "" ) {
                    		if (forceEncode === 0 ) {
                                    sc.innerHTML=nhttp.responseText;
                    		} else {
                        	    sc.innerHTML= Base64.decode(nhttp.responseText);
                    		}
                	}
            	  }
    		};
    		nhttp.open("POST",src, true);

		document.head.appendChild(sc);
	  } else {
		  logger(0,"already added :"+src);
	  }
	} else {
		logger(0,"fail to add script : "+src);
	}
}
var lastnav=null;
var lastsub=null;
var us="";
var au="";
function call_sub(obj) {
	lastsub=window.top["lastsub"];	
	if (  lastsub  !== null ) {
		 var li=document.getElementById("l"+lastsub.id.substr(1) );
		 if ( li ) {
                        li.setAttribute("style", "background-color:#6A6A6A;");
		        showUp( document.getElementById("s"+lastsub.id.substr(1) ) ) ;			 
		 }
	}
	if ( obj ) {
                var li=document.getElementById("l"+obj.id.substr(1) );
                if ( li ) {
                        li.setAttribute("style", "background-color:#abcA6A;");
			showUp( document.getElementById("s"+obj.id.substr(1) ) );
		}
		window.top["lastsub"]=obj;
	}
}
function call(obj,inf) {
	us=window.top["user"];
	au=window.top["auth"];
	if ( inf === "sub" ) {
		call_sub(obj);
		return;
	}
	lastnav=window.top["lastnav"];
	if ( lastnav !== null ) {
		 var li=document.getElementById("l"+lastnav.id.substr(1) );
                if ( li ) {
			li.setAttribute("style", "background-color:#6A6A6A;");
			showUp( document.getElementById("s"+lastnav.id.substr(1) ) ) ;
                }

	}
        if ( obj ) {
                var li=document.getElementById("l"+obj.id.substr(1) );
		if ( li ) {
			li.setAttribute("style", "background-color:#abcA6A;");
			showUp( document.getElementById("s"+obj.id.substr(1) ) );
		}
		var sp = obj.href.split("#");
		window.top["ref"]=sp[ sp.length - 1 ]+".html";
		window.top["loadContext"]("Page.jsp", "page="+sp[ sp.length - 1 ]+"&user="+us+"&auth="+au, "inner");
		window.top["lastnav"]=obj;
        }
}

function showUp(x) {
  if ( x ) {
    window.top["logger"](4,"found x:"+x.id);
   if (x.style.display === "none") {
       x.setAttribute("style", "display:block;");
       window.top["logger"](4,"show :"+x.id);
   } else {
       //x.style.display = "none";
       x.setAttribute("style", "display:none");
       window.top["logger"](4,"close :"+x.id);
   }
   window.top["logger"](3, "state: "+x.id+" ->"+x.style.display);
  } else {
      window.top["logger"](2,"not found "+x);
  }
}

function closeAllSub() {
     f=1;
	while ( document.getElementById("s"+f) ) {
		document.getElementById("s"+f).setAttribute("style", "display:none");
		f++;
	}
}

function openT(ev,like,tabc, tablinks ) {
	 window.top["openTab"](ev, like,tabc,tablinks);
}

function getFirstSubNav(id) {
	return "s"+id.substring(1)+"1";
}

function switchOn(tab,id){
   doc=document.getElementById(tab);
   if ( ! doc ) {
	var a = document.getElementById('topNav').getElementsByTagName('a');
	for (var i = 0; i < a.length; i++) {
    		var elem = a[i];
		if ( elem.title === tab ) { doc=elem; i=a.leggth; }
	}

   }
   if ( doc ) {
	   doc.click();
	   if ( id !== "" && tab === "Creator" ) {
		    pickupSol=true;
		    var sub=getFirstSubNav(doc.id);
		    logger(0,"click now :"+sub+";" );
                    document.getElementById(sub).click();
		    wait4SelectCount('SelSolutions', 2);
		    clickOnSelect('SelSolutions', id, 5000);
                    //loader("View.jsp", "action=pickupSolution&id="+id+"&user="+user+"&auth="+auth,"");
	   }
   }
}
