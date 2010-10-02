import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class MainAdmin extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="MainAdmin";

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
      if(Routines.loginCheck(true,request,response,database,context))
        {
        return;
        }
      Routines.WriteHTMLHead(null,//title
                             false,//showMenu
                             13,//menuHighLight
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
      webPageOutput.println("<CENTER>");
      webPageOutput.println("<IMG SRC=\"../Images/Admin.gif\"" +
                            " WIDTH='125' HEIGHT='115' ALT='Admin'>");
      webPageOutput.println("</CENTER>");
      Routines.tableStart(true,webPageOutput);
      Routines.tableHeader("Main Admin Menu",0,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      Routines.WriteHTMLLink(request,
                             response,
                             webPageOutput,
                             "CreateLeague",
                             null,
                             "Create League",
                             null,
                             true);
      Routines.tableDataEnd(true,true,true,webPageOutput);
	  Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
	  Routines.WriteHTMLLink(request,
							 response,
							 webPageOutput,
							 "ChangeLog",
							 null,
							 "Change Log",
							 null,
							 true);
	  Routines.tableDataEnd(true,true,true,webPageOutput);
      Routines.tableDataStart(true,true,true,true,false,0,0,"scoresrow",webPageOutput);
      Routines.WriteHTMLLink(request,
                             response,
                             webPageOutput,
                             "ViewLog",
                             null,
                             "View System Log",
                             null,
                             true);
      Routines.tableDataEnd(true,true,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(22));
      Routines.WriteHTMLTail(request,response,webPageOutput);
      }
   }