import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class Exit extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="Exit";

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
      session.removeAttribute("userName");
      session.removeAttribute("password");
      session.removeAttribute("coachNumber");
      session.removeAttribute("administrator");
      session.removeAttribute("redirect");
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
                             false,//showMenu
                             8,//menuHighLight
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
      Cookie[] cookies=request.getCookies();
      String cookieUserName="";
      String cookiePassword="";
      if(cookies!=null)
        {
        for(int currentCookie=0;currentCookie<cookies.length;currentCookie++)
          {
          if(cookies[currentCookie].getName().equals("userName"))
            {
            cookies[currentCookie].setMaxAge(0);
            response.addCookie(cookies[currentCookie]);
            }
          }
        }
      webPageOutput.println("<CENTER>");
      webPageOutput.println("<IMG SRC=\"../Images/Goodbye.gif\"" +
                            " WIDTH='120' HEIGHT='120' ALT='Goodbye'>");
      webPageOutput.println("</CENTER>");
      webPageOutput.println(Routines.spaceLines(1));
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Goodbye",0,webPageOutput);
      Routines.tableDataStart(true,true,false,true,true,0,0,"scoresrow",webPageOutput);
      webPageOutput.println("You have been signed out of the system.");
      Routines.tableDataEnd(false,true,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(17));
      Routines.WriteHTMLTail(request,response,webPageOutput);
      }
   }