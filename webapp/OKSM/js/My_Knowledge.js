
function getKMList(sobj,target,target2,typ){

    sid=sobj.options[ sobj.selectedIndex].value;
   snam=sobj.name;

	t=document.getElementById(target);
    tid = t.options[ t.selectedIndex].value;
    tnam= t.name;

	 t=document.getElementById(target2);
    pid = t.options[ t.selectedIndex].value;
    pnam= t.name;


	t=document.getElementById(typ);
    fid =  t.options[ t.selectedIndex].value;
    fnam= t.name;
   //alert(sid+"["+snam+"] t1="+target+":"+tid+"["+tnam+"] t2="+target2+":"+pid+"0["+pnam+"] typ:"+typ+":"+fid+"["+fnam+"]");

    loader('View.jsp', 'action=loadKM&typ=manager&'+snam+'='+sid+"&"+tnam+'='+tid+"&"+pnam+'='+pid+"&"+fnam+'='+fid+"&", '');
	
}

function onPageUnload(){
}

function onPageLoad() {
}

function loadInfo(area) {
  switch (area) {
 	 case "WhatMeans":   
		  u=document.getElementById('typ');
		  if ( u.options.length <= 1 ) {
		  	o=document.getElementById('fokus');
			if ( o ) {
				for( i=1; i<o.options.length; i++ ) {
					var opt = document.createElement('option');
		  			    opt.value=o.options[i].value;
		  			    opt.innerHTML=o.options[i].innerHTML;
					u.appendChild(opt);
				}
			}
		  }
		  o=document.getElementById('ulist');
		  if ( o && o.options[o.selectedIndex].value === '-1' ) {
		  	updateUserList(o, 'ulist');
		  }
    	 break;
	 default:
    // Anweisungen werden ausgeführt,
    // falls keine der case-Klauseln mit expression übereinstimmt
    	break;
  }
}

function updateUserList(obj,upd) {
	pl=getSelected('plist', "value");
	u="";
	if ( pl !== "" ) { u="&areatyp="+pl; }
	loader("View.jsp", "action=getAllUser&typ=manager&from="+obj.options[obj.selectedIndex].value+"&target="+upd+u, "");
}

function updateUserKM(obj,area,upd) {
	loader("View.jsp", "action=getCompTeam&typ=manager&from="+area.options[area.selectedIndex].value+"&with="+obj.options[obj.selectedIndex].value+"&target="+upd, "");	
}

function updateUserKM2(pl,area,typ,upd) {
	// updateUserKM2('plist', ulist' , 'typ', upd)
	v="value";
        loader("View.jsp", "action=getCompTeam&typ=manager&from="+getSelected(typ ,v)+"&with="+getSelected(area ,v)+"&target="+upd+"&areatyp="+getSelected(pl, v), "");
}

