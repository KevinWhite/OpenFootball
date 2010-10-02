import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class LeagueAdmin extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="LeagueAdmin";

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
      Routines.WriteHTMLHead("League Admin Menu",//title
                             true,//showMenu
                             12,//menuHighLight
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
      webPageOutput.println("<CENTER>");
      webPageOutput.println("<IMG SRC=\"../Images/Admin.gif\"" +
                            " WIDTH='125' HEIGHT='115' ALT='Admin'>");
      webPageOutput.println("</CENTER>");
      int league=Routines.safeParseInt((String)session.getAttribute("league"));
      int season=Routines.safeParseInt((String)session.getAttribute("season"));
      int week=Routines.safeParseInt((String)session.getAttribute("week"));
      pool.returnConnection(database);
      Routines.tableStart(true,webPageOutput);
      Routines.tableHeader("League Admin Menu",0,webPageOutput);
      Routines.tableDataStart(true,true,true,true,true,0,0,"scoresrow",webPageOutput);
      Routines.messageCheck(true,request,webPageOutput);
      Routines.WriteHTMLLink(request,
                             response,
                             webPageOutput,
                             "DeleteLeague",
                             "league=" +
                             league,
                             "Delete League",
                             null,
                             true);
      Routines.tableDataEnd(true,false,true,webPageOutput);
     // tempResults Button if required
     try
        {
        Statement sql = database.createStatement();
        ResultSet rs = sql.executeQuery("SELECT Results_ID " +
                                        "FROM tempresults");
        if (rs.first())
           {
           Routines.tableDataStart(true,true,true,true,true,0,0,"scoresrow",webPageOutput);
           Routines.WriteHTMLLink(request,response,
                                  webPageOutput,
                                  "ProcessResults",
                                  "league=" +
                                  league,
                                  "Load Results",
                                  null,
                                  true);
            Routines.tableDataEnd(true,false,true,webPageOutput);
            }
         }
     catch(SQLException error)
         {
		 Routines.writeToLog(servletName,"Database error checking for tempResults : " + error,false,context);	
         }
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(22));
      Routines.WriteHTMLTail(request,response,webPageOutput);
      }
   }