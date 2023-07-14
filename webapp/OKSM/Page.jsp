<%-- 
    Document   : View
    Created on : 06.06.2023, 16:54:42
    Author     : SuMario
--%>

<%@page import="com.macmario.oksm.Oksm"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>

<%
    HttpSession sess = request.getSession();
    if ( sess == null ) {  sess = request.getSession(); }
    Oksm oksm = (Oksm) sess.getAttribute("oksm");
    if ( oksm == null ){
         oksm = new Oksm(request);
         sess.setAttribute("oksm",oksm);
    } else {
        oksm.updateRequest(request);
    }
    
    oksm.getPage(request,response);
    
%>


