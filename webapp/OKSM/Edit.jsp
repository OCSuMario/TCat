<%-- 
    Document   : Edit.jsp
    Created on : 16.06.2023, 09:03:17
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
    oksm.update(request,response);

%>
