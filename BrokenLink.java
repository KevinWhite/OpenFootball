import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class BrokenLink extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="BrokenLink";

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
      Routines.WriteHTMLHead("Missing Page",//title
                             true,//showMenu
                             0,//menuHighLight
                             false,//seasonsMenu
                             false,//weeksMenu
                             false,//scores
                             false,//standings
                             false,//gameCenter
                             false,//Schedules
                             false,//previews
                             false,//teamCenter
		                     false,//draft
                             database,//database
                             request,//request
                             response,//response
                             webPageOutput,//webPageOutput
                             context);//context
      pool.returnConnection(database);
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Apologies",0,webPageOutput);
      Routines.tableDataStart(true,false,false,true,true,0,0,"scoresrow",webPageOutput);
      webPageOutput.println("You have encountered a page which has not yet been setup ");
      webPageOutput.println("correctly. Details of this missing page have now been ");
      webPageOutput.println("logged and will be rectified as soon as possible.");
      webPageOutput.println(Routines.clubPitSig());
      Routines.tableDataEnd(false,true,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(15));
      Routines.WriteHTMLTail(request,response,webPageOutput);
      Routines.writeToLog(servletName,"Broken Link, Action=" + request.getParameter("action"),false,context);
      }
   }