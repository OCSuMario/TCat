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
    
    if ( oksm != null ) {
       oksm.updateRequest(request);
       oksm.setAuth(request,response);
    } else {
        System.out.println("no oskm");
    }
%>


