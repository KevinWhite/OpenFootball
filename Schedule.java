import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class Schedule extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="Schedule";

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
      Routines.WriteHTMLHead("Schedules",//title
                                    true,//showMenu
                                    4,//menuHighLight
                                    true,//seasonsMenu
		                            true,//weeksMenu
                                    true,//scores
                                    false,//standings
                                    false,//gameCenter
                                    true,//schedules
                                    false,//previews
                                    false,//teamCenter
		                            false,//draft
                                    database,//database
                                    request,//request
                                    response,//response
                                    webPageOutput,//webPageOutput
                                    context);//context
      int league=Routines.safeParseInt((String)session.getAttribute("league"));
      int season=Routines.safeParseInt((String)session.getAttribute("season"));
      Routines.titleHeader("head2",true,webPageOutput);
      webPageOutput.println("<B>Season " + season + " Schedule</B>");
      Routines.titleTrailer(true,webPageOutput);
      webPageOutput.println("<DIV CLASS=\"SLTables1\">");

      try
         {
         Statement sql = database.createStatement();
         ResultSet queryResponse = sql.executeQuery("SELECT FixtureNumber,FinalQuarter,AwayTeam,HomeTeamScore,AwayTeamScore,Name,Week " +
                                                    "FROM   fixtures,teams " +
                                                    "WHERE  LeagueNumber = " + league + " " +
                                                    "AND    Season = " + season + " " +
                                                    "AND    TeamNumber = HomeTeam " +
                                                    "ORDER  BY Week ASC,Game ASC");
         int currentWeek=0;
         boolean flipFlop=false;
         while(queryResponse.next())
            {
            int fixtureNumber   = queryResponse.getInt(1);
            int finalQuarter    = queryResponse.getInt(2);
            int awayTeam        = queryResponse.getInt(3);
            int homeTeamScore   = queryResponse.getInt(4);
            int awayTeamScore   = queryResponse.getInt(5);
            String homeTeamName = queryResponse.getString(6);
            int week            = queryResponse.getInt(7);
            Statement sql2 = database.createStatement();
            ResultSet queryResponse2 = sql2.executeQuery("SELECT Name " +
                                                         "FROM   teams " +
                                                         "WHERE  TeamNumber = " + awayTeam);
            queryResponse2.first();
            String awayTeamName = queryResponse2.getString(1);
            if(currentWeek!=week)
              {
              if(currentWeek!=0)
                {
                webPageOutput.println("</TBODY>");
                webPageOutput.println("</TABLE>");
                webPageOutput.println("</DIV>");
                webPageOutput.println("<BR>");
                webPageOutput.println("<TABLE WIDTH=\"90%\">");
                webPageOutput.println("<TBODY>");
                webPageOutput.println("<TR WIDTH=\"100%\" ALIGN=\"right\">");
                webPageOutput.println("<TD><A HREF=\"#top\">Return to top</A></TD>");
                webPageOutput.println("</TR>");
                webPageOutput.println("</TBODY>");
                webPageOutput.println("</TABLE>");
                webPageOutput.println("<BR>");
                }
              webPageOutput.println("<A NAME=\"Week" + week + "\"></A>");
              webPageOutput.println("<DIV CLASS=\"SLTables2\">");
              webPageOutput.println("<TABLE WIDTH=\"100%\" CELLPADDING=\"2\" CELLSPACING=\"1\" BORDER=\"0\">");
              webPageOutput.println("<TBODY>");
              webPageOutput.println("<TR ALIGN=\"center\" CLASS=\"bg0\">");
              webPageOutput.println("<TD CLASS=\"bg0\" COLSPAN=\"2\">");
              webPageOutput.println("<FONT CLASS=\"bg0font\">");
              webPageOutput.println(Routines.decodeWeekNumber(Routines.safeParseInt((String)session.getAttribute("preSeasonWeeks")),
                                                                     Routines.safeParseInt((String)session.getAttribute("regularSeasonWeeks")),
                                                                     Routines.safeParseInt((String)session.getAttribute("postSeasonWeeks")),
                                                                     season,
                                                                     week,
                                                                     false,
                                                                     session));
              webPageOutput.println("</FONT>");
              webPageOutput.println("</TD>");
              webPageOutput.println("</TR>");
              webPageOutput.println("<TR ALIGN=\"center\" CLASS=\"bg1\">");
              webPageOutput.println("<TD CLASS=\"bg1\" COLSPAN=\"2\">");
              webPageOutput.println("<FONT CLASS=\"bg1font\"><B>");
              webPageOutput.println("Sunday");
              webPageOutput.println("</B></FONT>");
              webPageOutput.println("</TD>");
              webPageOutput.println("</TR>");
              currentWeek=week;
              flipFlop=false;
              }
            if(!flipFlop)
              {
              webPageOutput.println("<TR HEIGHT=\"17\" CLASS=\"bg2\" ALIGN=\"right\">");
              }
            webPageOutput.println("<TD ALIGN=\"left\" WIDTH=\"50%\">");
            if (finalQuarter>0)
               {
               String text = awayTeamName + " " + awayTeamScore + ", " + homeTeamName + " " + homeTeamScore;
               Routines.WriteHTMLLink(request,
                                             response,
                                             webPageOutput,
                                             "wfafl",
                                             "action=viewRecap" +
                                             "&value=" +
                                             fixtureNumber,
                                             text,
                                             null,
                                             true);
               }
            else
               {
               String text = awayTeamName + " at " + homeTeamName;
               Routines.WriteHTMLLink(request,
                                             response,
                                             webPageOutput,
                                             "Preview",
                                             "league=" +
                                             league +
                                             "&season=" +
                                             season +
                                             "&week=" +
                                             week +
                                             "&fixture=" +
                                             fixtureNumber,
                                             text,
                                             null,
                                             true);
               }
            webPageOutput.println("</TD>");
            if(flipFlop)
              {
              webPageOutput.println("</TR>");
              }
            if(flipFlop)
              {
              flipFlop=false;
              }
            else
              {
              flipFlop=true;
              }
            }
         if(currentWeek!=0)
           {
           webPageOutput.println("</TBODY>");
           webPageOutput.println("</TABLE>");
           webPageOutput.println("</DIV>");
           webPageOutput.println("<BR>");
           webPageOutput.println("<TABLE WIDTH=\"90%\">");
           webPageOutput.println("<TBODY>");
           webPageOutput.println("<TR WIDTH=\"100%\" ALIGN=\"right\">");
           webPageOutput.println("<TD><A HREF=\"#top\">Return to top</A></TD>");
           webPageOutput.println("</TR>");
           webPageOutput.println("</TBODY>");
           webPageOutput.println("</TABLE>");
           webPageOutput.println("<BR>");
           }
         }
      catch(SQLException error)
         {
		 Routines.writeToLog(servletName,"Database error retrieving fixtures : " + error,false,context);	
         }
      finally
         {
         pool.returnConnection(database);
         }
      webPageOutput.println("</DIV>");
      Routines.WriteHTMLTail(request,response,webPageOutput);
      }
   }