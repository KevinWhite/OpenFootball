import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class Contact extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="Contact";

   public void init()
      {
      context = getServletContext();
      synchronized(context)
        {
        pool=(ConnectionPool)context.getAttribute("pool");
        if(pool==null)
          {
          String driverClassName = context.getInitParameter("driverClassName");
          String url = context.getInitParameter("url");
          String userName = context.getInitParameter("username");
          String password = context.getInitParameter("password");
          try
            {
            pool=new ConnectionPool(driverClassName,url,userName,password);
            }
          catch(Exception error)
            {
            Routines.writeToLog(servletName,"Unable to create connection pool : " + error,false,context);
            }
          context.setAttribute("pool",pool);
          }
        }
      }

   public void doGet(HttpServletRequest request,
                     HttpServletResponse response)
      {
      response.setContentType("text/html");
      PrintWriter webPageOutput=null;
      try
        {
        webPageOutput=response.getWriter();
        }
      catch(IOException error)
        {
        Routines.writeToLog(servletName,"Error getting writer : " + error,false,context);
        }
      HttpSession session=request.getSession();
      session.setAttribute("redirect",request.getRequestURL() + "?" + request.getQueryString());
      Connection database=null;
      try
        {
        database=pool.getConnection(servletName);
        }
      catch(SQLException error)
        {
        Routines.writeToLog(servletName,"Unable to connect to database : " + error,false,context);
        }
      Routines.WriteHTMLHead(null,//title
                             true,//showMenu
                             0,//menuHighLight
                             false,//seasonsMenu
		                     false,//weeksMenu
                             false,//scores
                             false,//standings
                             false,//gameCenter
                             false,//schedules
                             false,//previews
                             false,//teamCenter
		                     false,//draft
                             database,//database
                             request,//request
                             response,//response
                             webPageOutput,//webPageOutput
                             context);//context
      pool.returnConnection(database);
      webPageOutput.println(Routines.spaceLines(1));
      webPageOutput.println("<CENTER>");
      webPageOutput.println("<IMG SRC=\"../Images/Contact.gif\"" +
                            " WIDTH='184' HEIGHT='40' ALT='Contact'>");
      webPageOutput.println("</CENTER>");
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Support",0,webPageOutput);
      Routines.tableDataStart(true,true,false,true,true,0,0,"scoresrow",webPageOutput);
      webPageOutput.println("<A HREF=\"mailto:support@clubpit.com\">support@clubpit.com</A>");
      Routines.tableDataEnd(false,true,true,webPageOutput);
      Routines.tableHeader("Feedback",0,webPageOutput);
      Routines.tableDataStart(true,true,false,true,true,0,0,"scoresrow",webPageOutput);
      webPageOutput.println("<A HREF=\"mailto:feedback@clubpit.com\">feedback@clubpit.com</A>");
      Routines.tableDataEnd(false,true,true,webPageOutput);
      Routines.tableHeader("Bugs",0,webPageOutput);
      Routines.tableDataStart(true,true,false,true,true,0,0,"scoresrow",webPageOutput);
      webPageOutput.println("<A HREF=\"mailto:bugs@clubpit.com\">bugs@clubpit.com</A>");
      Routines.tableDataEnd(false,true,true,webPageOutput);
      Routines.tableHeader("Complaints",0,webPageOutput);
      Routines.tableDataStart(true,true,false,true,true,0,0,"scoresrow",webPageOutput);
      webPageOutput.println("<A HREF=\"mailto:complaints@clubpit.com\">complaints@clubpit.com</A>");
      Routines.tableDataEnd(false,true,true,webPageOutput);
      Routines.tableHeader("General",0,webPageOutput);
      Routines.tableDataStart(true,true,false,true,true,0,0,"scoresrow",webPageOutput);
      webPageOutput.println("<A HREF=\"mailto:general@clubpit.com\">general@clubpit.com</A>");
      Routines.tableDataEnd(false,true,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(10));
      Routines.WriteHTMLTail(request,response,webPageOutput);
      }
   }