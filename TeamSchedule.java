import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.text.DateFormat;

public class TeamSchedule extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="TeamSchedule";

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
      Routines.WriteHTMLHead("Team Schedule",//title
                             true,//showMenu
                             6,//menuHighLight
                             true,//seasonsMenu
		                     false,//weeksMenu
                             false,//scores
                             false,//standings
                             false,//gameCenter
                             false,//schedules
                             false,//previews
                             true,//teamCenter
		                     false,//draft
                             database,//database
                             request,//request
                             response,//response
                             webPageOutput,//webPageOutput
                             context);//context
      int league=Routines.safeParseInt((String)session.getAttribute("league"));
      int season=Routines.safeParseInt((String)session.getAttribute("season"));
      int team=Routines.safeParseInt((String)session.getAttribute("team"));
      Routines.titleHeader("head2",true,webPageOutput);
	  webPageOutput.println(Routines.spaceLines(1));
	  webPageOutput.println("<B>Schedule</B>");
      Routines.titleTrailer(true,webPageOutput);
	  webPageOutput.println("<DIV CLASS=\"SLTables1\">");
	  int startOfRegularSeason=0;
	  int startOfPostSeason=0;
	  int finalWeek=0;
	  int homeTeam=0;
	  int awayTeam=0;
	  Calendar seasonStartCal=Calendar.getInstance();
	  try
        {
		Statement sql = database.createStatement();	
		ResultSet queryResponse = sql.executeQuery("SELECT PreSeasonWeeks,RegularSeasonWeeks,PostSeasonWeeks,SeasonStart " +
												   "FROM   leagues " +
												   "WHERE  LeagueNumber = " + league);
		if(queryResponse.first())
		  {
		  startOfRegularSeason=queryResponse.getInt(1);
		  startOfPostSeason=queryResponse.getInt(1)+queryResponse.getInt(2);
		  finalWeek=queryResponse.getInt(1)+queryResponse.getInt(2)+queryResponse.getInt(3)-1;	
		  java.util.Date seasonStart =queryResponse.getDate(4);
		  seasonStartCal.setTime(seasonStart);
		  }
        }
	  catch(SQLException error)
		{
		Routines.writeToLog(servletName,"Database error retrieving league : " + error,false,context);	
		}       
      try
         {
         Statement sql = database.createStatement();
         ResultSet queryResponse = sql.executeQuery("SELECT FixtureNumber,FinalQuarter,HomeTeam,AwayTeam,HomeTeamScore,AwayTeamScore,Name,Week " +
                                                    "FROM   fixtures,teams " +
                                                    "WHERE  LeagueNumber = " + league + " " +
                                                    "AND    Season = " + season + " " +
                                                    "AND    (HomeTeam = " + team + " OR AwayTeam = " + team + ") " +
                                                    "AND    TeamNumber = HomeTeam " +
                                                    "ORDER  BY Week ASC,Game ASC");
         int currentWeek=0;
         while(queryResponse.next())
            {
            seasonStartCal.add(Calendar.DATE,7);	
            int fixtureNumber   = queryResponse.getInt(1);
            int finalQuarter    = queryResponse.getInt(2);
            homeTeam        = queryResponse.getInt(3);
            awayTeam        = queryResponse.getInt(4);
            int homeTeamScore   = queryResponse.getInt(5);
            int awayTeamScore   = queryResponse.getInt(6);
            String homeTeamName = queryResponse.getString(7);
            int week            = queryResponse.getInt(8);
            Statement sql2 = database.createStatement();
            ResultSet queryResponse2 = sql2.executeQuery("SELECT Name " +
                                                         "FROM   teams " +
                                                         "WHERE  TeamNumber = " + awayTeam);
            queryResponse2.first();
            String awayTeamName = queryResponse2.getString(1);
            if(currentWeek!=week)
              {
              if(week==1||week==startOfRegularSeason+1||week==startOfPostSeason+1||week==startOfPostSeason+1)
                {
				if(currentWeek!=0)
				  {
				  webPageOutput.println("</TBODY>");
				  webPageOutput.println("</TABLE>");
				  webPageOutput.println("</DIV>");
				  webPageOutput.println("<BR>");
				  webPageOutput.println("<BR>");
				  }
                webPageOutput.println("<A NAME=\"Week" + week + "\"></A>");
                webPageOutput.println("<DIV CLASS=\"SLTables1\">");
                webPageOutput.println("<TABLE WIDTH=\"100%\" CELLPADDING=\"2\" CELLSPACING=\"1\" BORDER=\"0\">");
                webPageOutput.println("<TBODY>");
			    webPageOutput.println("<TR ALIGN=\"center\" CLASS=\"home\">");
			    webPageOutput.println("<TD CLASS=\"home\" COLSPAN=\"4\">");
                webPageOutput.println("<FONT CLASS=\"bg0font\">");
                if(week==1)
                  {
                  webPageOutput.println("Season "+season+" PreSeason");	
                  }
				if(week==startOfRegularSeason+1)
				  {
				  webPageOutput.println("Season "+season+" Regular Season");	
				  }   
				if(week==startOfPostSeason+1)
				  {
				  webPageOutput.println("Season "+season+" Post Season");	
				  }  
				if(week==startOfPostSeason+1)
				  {
				  webPageOutput.println("Season "+season+" Final");	
				  } 				   				                 
                webPageOutput.println("</FONT>");
                webPageOutput.println("</TD>");
                webPageOutput.println("</TR>");
                webPageOutput.println("<TR ALIGN=\"center\" CLASS=\"bg1\">");
                webPageOutput.println("<TD CLASS=\"bg1\" COLSPAN=\"1\">");
                webPageOutput.println("<FONT CLASS=\"bg1font\"><B>");
                webPageOutput.println("Week");
                webPageOutput.println("</B></FONT>");
				webPageOutput.println("<TD CLASS=\"bg1\" COLSPAN=\"1\">");
				webPageOutput.println("<FONT CLASS=\"bg1font\"><B>");
				webPageOutput.println("Date");
				webPageOutput.println("</B></FONT>");
				webPageOutput.println("<TD CLASS=\"bg1\" COLSPAN=\"1\">");
				webPageOutput.println("<FONT CLASS=\"bg1font\"><B>");
				webPageOutput.println("Opponent");
				webPageOutput.println("</B></FONT>");
				webPageOutput.println("<TD CLASS=\"bg1\" COLSPAN=\"1\">");
				webPageOutput.println("<FONT CLASS=\"bg1font\"><B>");
				webPageOutput.println("Result");
				webPageOutput.println("</B></FONT>");
                webPageOutput.println("</TD>");
                webPageOutput.println("</TR>");
                currentWeek=week;
                }
              }
            webPageOutput.println("<TR HEIGHT=\"17\" CLASS=\"bg2\" ALIGN=\"right\">");
			webPageOutput.println("<TD ALIGN=\"left\" WIDTH=\"25%\">");
			webPageOutput.println(Routines.decodeWeekNumber(Routines.safeParseInt((String)session.getAttribute("preSeasonWeeks")),
																				 Routines.safeParseInt((String)session.getAttribute("regularSeasonWeeks")),
																				 Routines.safeParseInt((String)session.getAttribute("postSeasonWeeks")),
																				 season,
																				 week,
																				 false,
																				 session));
			webPageOutput.println("</TD>");
			webPageOutput.println("<TD ALIGN=\"left\" WIDTH=\"25%\">");
			int dayOfWeek=seasonStartCal.get(Calendar.DAY_OF_WEEK);
            switch(dayOfWeek)
                  {
                  case 1: webPageOutput.println("Sunday "); break;
				  case 2: webPageOutput.println("Monday "); break;
				  case 3: webPageOutput.println("Tuesday "); break;
				  case 4: webPageOutput.println("Wednesday "); break;
				  case 5: webPageOutput.println("Thursday "); break;
				  case 6: webPageOutput.println("Friday "); break;
				  case 7: webPageOutput.println("Saturday "); break;
                  }
			webPageOutput.println(DateFormat.getDateInstance().format(seasonStartCal.getTime()));
			webPageOutput.println("</TD>");
            webPageOutput.println("<TD ALIGN=\"left\" WIDTH=\"25%\">");
            String text="";
            if(homeTeam==team)
              {
			  text = awayTeamName;	
              }
            else
              {
			  text = "@"+homeTeamName;  
              }
            if (finalQuarter>0)
               {
               text = awayTeamName + " " + awayTeamScore + ", " + homeTeamName + " " + homeTeamScore;
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
			webPageOutput.println("<TD ALIGN=\"left\" WIDTH=\"25%\">");
			webPageOutput.println("</TD>");
            webPageOutput.println("</TR>");
            }
         if(currentWeek!=0)
           {
           webPageOutput.println("</TBODY>");
           webPageOutput.println("</TABLE>");
           webPageOutput.println("</DIV>");
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