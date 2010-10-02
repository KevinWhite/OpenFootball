import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class Scores extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="Scores";

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
      Routines.WriteHTMLHead("Scores",//title
                                    true,//showMenu
                                    2,//menuHighLight
                                    true,//seasonsMenu
		                            true,//weeksMenu
                                    true,//scores
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
      //Work out if selected week has been played yet or not.
        int viewWeek=Routines.safeParseInt((String)session.getAttribute("viewWeek"));
        if (Routines.safeParseInt((String)session.getAttribute("week"))==0)
           {
           Previews.createScreen(request,
                                  response,
                                  database,
                                  webPageOutput,
                                  context);
           }
        else
           {
           int latestSeason=Routines.safeParseInt((String)session.getAttribute("season"));
           int latestWeek=Routines.safeParseInt((String)session.getAttribute("week"));
           int viewSeason=Routines.safeParseInt((String)session.getAttribute("season"));
           if(latestSeason==viewSeason&&latestWeek<viewWeek)
             {
             Previews.createScreen(request,
                                   response,
                                   database,
                                   webPageOutput,
                                   context);
             }
          else
             {
             Results.createScreen(request,
                                  response,
                                  database,
                                  webPageOutput,
                                  context);
             }
           }
      pool.returnConnection(database);
      }
   }