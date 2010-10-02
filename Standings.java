import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class Standings extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="Standings";

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
      Routines.WriteHTMLHead("Standings",//title
                                    true,//showMenu
                                    5,//menuHighLight
                                    true,//seasonsMenu
		                            true,//weeksMenu
                                    false,//scores
                                    true,//standings
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
      int league=Routines.safeParseInt((String)session.getAttribute("league"));
      int latestSeason=Routines.safeParseInt((String)session.getAttribute("season"));
      int latestWeek=Routines.safeParseInt((String)session.getAttribute("week"));
      int viewSeason=Routines.safeParseInt((String)session.getAttribute("viewSeason"));
      int viewWeek=Routines.safeParseInt((String)session.getAttribute("viewWeek"));
      // Prepare screen
      Routines.titleHeader("head2",true,webPageOutput);
      int standingsWeek=viewWeek;
      if(latestWeek==0)
         {
         standingsWeek=0;
         }
      ProcessResults.playOffs(league,viewSeason,viewWeek,database,context);
      webPageOutput.println("Standings for Season " +
                            viewSeason +
                            ", " +
                            Routines.decodeWeekNumber(Routines.safeParseInt((String)session.getAttribute("preSeasonWeeks")),
                                                      Routines.safeParseInt((String)session.getAttribute("regularSeasonWeeks")),
                                                      Routines.safeParseInt((String)session.getAttribute("postSeasonWeeks")),
                                                      viewSeason,
                                                      standingsWeek,
                                                      true,
                                                      session));
      Routines.titleTrailer(true,webPageOutput);
      // Retrieve Standings information from DB
      try
         {
         Statement sql=database.createStatement();
         ResultSet rs=null;
         rs=sql.executeQuery("SELECT LeagueType " +
                             "FROM leagues " +
                             "WHERE LeagueNumber=" +league);
         int leagueType=-1;
         if(rs.first())
           {
           leagueType=rs.getInt(1);	                    
           }
         if(latestSeason==viewSeason&&latestWeek==viewWeek)
           {
           rs=sql.executeQuery("SELECT conferences.ConferenceNumber," +
                               "conferences.ConferenceName," +
                               "divisions.DivisionNumber," +
                               "divisions.DivisionName," +
                               "teams.TeamNumber," +
                               "teams.Name," +
                               "Wins," +
                               "Losses," +
                               "Draws," +
                               "Scored," +
                               "Conceded," +
                               "HomeWins," +
                               "HomeLosses," +
                               "HomeDraws," +
                               "AwayWins," +
                               "AwayLosses," +
                               "AwayDraws," +
                               "DivisionWins," +
                               "DivisionLosses," +
                               "DivisionDraws," +
                               "ConferenceWins," +
                               "ConferenceLosses," +
                               "ConferenceDraws," +
                               "InterWins," +
                               "InterLosses," +
                               "InterDraws," +
                               "ForeNames," +
                               "Surname," +
                               "coaches.CoachNumber," +
                               "Streak," +
                               "AutoCoach " +
                               "FROM standings," +
                               "teams," +
                               "leagueteams," +
                               "divisions," +
                               "conferences," +
                               "coachteams," +
                               "coaches " +
                               "WHERE standings.TeamNumber       = teams.TeamNumber " +
                               "AND   teams.TeamNumber           = leagueteams.TeamNumber " +
                               "AND   leagueteams.DivisionNumber = divisions.DivisionNumber " +
                               "AND   divisions.ConferenceNumber = conferences.ConferenceNumber " +
                               "AND   conferences.LeagueNumber   = " + league + " " +
                               "AND   standings.Season           = " + viewSeason + " " +
                               "AND   standings.Week             = " + standingsWeek + " " +
                               "AND   standings.TeamNumber       = coachteams.TeamNumber " +
                               "AND   coachteams.CoachNumber     = coaches.CoachNumber " +
                               "ORDER BY conferences.ConferenceNumber ASC," +
                               "divisions.DivisionNumber ASC," +
                               "Position ASC");
           }
         else
           {
           rs=sql.executeQuery("SELECT conferences.ConferenceNumber," +
                               "conferences.ConferenceName," +
                               "divisions.DivisionNumber," +
                               "divisions.DivisionName," +
                               "teams.TeamNumber," +
                               "teams.Name," +
                               "Wins," +
                               "Losses," +
                               "Draws," +
                               "Scored," +
                               "Conceded," +
                               "HomeWins," +
                               "HomeLosses," +
                               "HomeDraws," +
                               "AwayWins," +
                               "AwayLosses," +
                               "AwayDraws," +
                               "DivisionWins," +
                               "DivisionLosses," +
                               "DivisionDraws," +
                               "ConferenceWins," +
                               "ConferenceLosses," +
                               "ConferenceDraws," +
                               "InterWins," +
                               "InterLosses," +
                               "InterDraws," +
                               "ForeNames," +
                               "Surname," +
                               "coaches.CoachNumber," +
                               "Streak," +
                               "AutoCoach " +
                               "FROM standings," +
                               "teams," +
                               "leagueteams," +
                               "divisions," +
                               "conferences," +
                               "coaches " +
                               "WHERE standings.TeamNumber       = teams.TeamNumber " +
                               "AND   teams.TeamNumber           = leagueteams.TeamNumber " +
                               "AND   leagueteams.DivisionNumber = divisions.DivisionNumber " +
                               "AND   divisions.ConferenceNumber = conferences.ConferenceNumber " +
                               "AND   conferences.LeagueNumber   = " + league + " " +
                               "AND   standings.Season           = " + viewSeason + " " +
                               "AND   standings.Week             = " + standingsWeek + " " +
                               "AND   standings.CoachNumber      = coaches.CoachNumber " +
                               "ORDER BY conferences.ConferenceNumber ASC," +
                               "divisions.DivisionNumber ASC," +
                               "Position ASC");
           }
         int     currentConference  = 0;
         boolean conferenceFlipFlop = false;
         String  conferenceColour   = new String("");
         int     currentDivision    = 0;
         boolean teamFlipFlop       = false;
         webPageOutput.println("<TABLE WIDTH=\"619\" CELLSPACING=\"1\" CELLPADDING=\"1\" BORDER=\"0\">");
         webPageOutput.println("<TBODY>");
         while(rs.next())
            {
            int    conferenceNumber = rs.getInt(1);
            String conferenceName   = new String(rs.getString(2));
            int    divisionNumber   = rs.getInt(3);
            String divisionName     = new String(rs.getString(4));
            int    teamNumber       = rs.getInt(5);
            String teamName         = new String(rs.getString(6));
            int    wins             = rs.getInt(7);
            int    losses           = rs.getInt(8);
            int    draws            = rs.getInt(9);
            int    scored           = rs.getInt(10);
            int    conceded         = rs.getInt(11);
            int    homeWins         = rs.getInt(12);
            int    homeLosses       = rs.getInt(13);
            int    homeDraws        = rs.getInt(14);
            int    awayWins         = rs.getInt(15);
            int    awayLosses       = rs.getInt(16);
            int    awayDraws        = rs.getInt(17);
            int    divisionWins     = rs.getInt(18);
            int    divisionLosses   = rs.getInt(19);
            int    divisionDraws    = rs.getInt(20);
            int    conferenceWins   = rs.getInt(21);
            int    conferenceLosses = rs.getInt(22);
            int    conferenceDraws  = rs.getInt(23);
            int    interWins        = rs.getInt(24);
            int    interLosses      = rs.getInt(25);
            int    interDraws       = rs.getInt(26);
            String foreNames        = new String(rs.getString(27));
            String surName          = new String(rs.getString(28));
            int    coachNumber      = rs.getInt(29);
            int    streak           = rs.getInt(30);
            int    autoCoach        = rs.getInt(31);
            if (currentConference != conferenceNumber)
               {
               currentConference = conferenceNumber;
               if (conferenceFlipFlop)
                  {
                  conferenceColour = "#004079";
                  conferenceFlipFlop = false;
                  }
               else
                  {
                  conferenceColour = "#b50023";
                  conferenceFlipFlop = true;
                  }
               }
            if (currentDivision != divisionNumber)
               {
               currentDivision = divisionNumber;
               String displayDivision="";
               if(leagueType==1)
                 {
                 displayDivision=conferenceName.substring(0,1) + "FC " + divisionName;	
                 }
			   if(leagueType==2)
				 {
				 displayDivision=conferenceName + " " + divisionName;	
				 }                 
               webPageOutput.println("<TR BGCOLOR=\"" + conferenceColour + "\">");
               webPageOutput.println("<TH COLSPAN=\"13\"><A NAME=\"" +
                                     displayDivision +
                                     "\"><FONT CLASS=\"white\">" +
                                     displayDivision +
                                     "</FONT></A></TH>");
               webPageOutput.println("</TR>");
               webPageOutput.println("<TR BGCOLOR=\"#d1d1d1\">");
               webPageOutput.println("<TH ALIGN=\"left\" WIDTH=\"120\" scope=\"col\">");
               webPageOutput.println("<FONT CLASS=\"table\">Team</FONT></TH>");
               webPageOutput.println("<TH ALIGN=\"left\" WIDTH=\"95\" scope=\"col\">");
               webPageOutput.println("<FONT CLASS=\"table\">Coach</FONT></TH>");
               webPageOutput.println("<TH ALIGN=\"center\" WIDTH=\"20\" SCOPE=\"col\"><FONT CLASS=\"table\">W</FONT></TH>");
               webPageOutput.println("<TH ALIGN=\"center\" WIDTH=\"20\" SCOPE=\"col\"><FONT CLASS=\"table\">L</FONT></TH>");
               webPageOutput.println("<TH ALIGN=\"center\" WIDTH=\"20\" SCOPE=\"col\"><FONT CLASS=\"table\">T</FONT></TH>");
               webPageOutput.println("<TH ALIGN=\"center\" WIDTH=\"30\" SCOPE=\"col\"><FONT CLASS=\"table\">PF</FONT></TH>");
               webPageOutput.println("<TH ALIGN=\"center\" WIDTH=\"30\" SCOPE=\"col\"><FONT CLASS=\"table\">PA</FONT></TH>");
               webPageOutput.println("<TH ALIGN=\"center\" WIDTH=\"43\" SCOPE=\"col\"><FONT CLASS=\"table\">Home</FONT></TH>");
               webPageOutput.println("<TH ALIGN=\"center\" WIDTH=\"43\" SCOPE=\"col\"><FONT CLASS=\"table\">Road</FONT></TH>");
               webPageOutput.println("<TH ALIGN=\"center\" WIDTH=\"43\" SCOPE=\"col\"><FONT CLASS=\"table\">Div</FONT></TH>");
               webPageOutput.println("<TH ALIGN=\"center\" WIDTH=\"43\" SCOPE=\"col\"><FONT CLASS=\"table\">Conf</FONT></TH>");
               webPageOutput.println("<TH ALIGN=\"center\" WIDTH=\"43\" SCOPE=\"col\"><FONT CLASS=\"table\">Inter</FONT></TH>");
               webPageOutput.println("<TH ALIGN=\"center\" SCOPE=\"col\"><FONT CLASS=\"table\">Streak</FONT></TH>");
               webPageOutput.println("</TR>");
               }
            if (teamFlipFlop)
               {
               webPageOutput.println("<TR>");
               teamFlipFlop = false;
               }
            else
               {
               webPageOutput.println("<TR BGCOLOR=\"#e5e5e5\">");
               teamFlipFlop = true;
               }
            String formattedName = new String(Routines.formatName(foreNames,surName,11));
            webPageOutput.println("<TD WIDTH=\"120\"><FONT CLASS=\"lead\">");
//                                  yz-
            Routines.WriteHTMLLink(request,
                                   response,
                                   webPageOutput,
                                   "TeamNews",
                                   "league=" +
                                   league +
                                   "&team=" +
                                   teamNumber,
                                   teamName,
                                   null,
                                   true);
            webPageOutput.println("</TD>");
            webPageOutput.println("<TD WIDTH=\"95\"><FONT CLASS=\"lead\">");
            if(autoCoach==1)
              {
              webPageOutput.println("<I>");
              }
            else
              {
              webPageOutput.println("<B>");
              }
            Routines.WriteHTMLLink(request,
                                          response,
                                          webPageOutput,
                                          "wfafl",
                                          "action=viewCoach" +
                                          "&value=" +
                                          coachNumber,
                                          formattedName,
                                          null,
                                          true);
            if(autoCoach==1)
              {
              webPageOutput.println("</I>");
              }
            else
              {
              webPageOutput.println("</B>");
              }
            webPageOutput.println("</FONT></TD>");
            webPageOutput.println("<TD ALIGN=\"right\" WIDTH=\"20\"><FONT CLASS=\"lead\">" + wins + "</FONT></TD>");
            webPageOutput.println("<TD ALIGN=\"right\" WIDTH=\"20\"><FONT CLASS=\"lead\">" + losses + "</FONT></TD>");
            webPageOutput.println("<TD ALIGN=\"right\" WIDTH=\"20\"><FONT CLASS=\"lead\">" + draws + "</FONT></TD>");
            webPageOutput.println("<TD ALIGN=\"right\" WIDTH=\"30\"><FONT CLASS=\"lead\">" + scored + "</FONT></TD>");
            webPageOutput.println("<TD ALIGN=\"right\" WIDTH=\"30\"><FONT CLASS=\"lead\">" + conceded + "</FONT></TD>");
            webPageOutput.print("<TD ALIGN=\"center\" WIDTH=\"43\"><FONT CLASS=\"lead\">" + homeWins + "-" + homeLosses);
            if (homeDraws!=0)
               {
               webPageOutput.print("-" + homeDraws);
               }
            webPageOutput.println("</FONT></TD>");
            webPageOutput.print("<TD ALIGN=\"center\" WIDTH=\"43\"><FONT CLASS=\"lead\">" + awayWins + "-" + awayLosses);
            if (awayDraws!=0)
               {
               webPageOutput.print("-" + awayDraws);
               }
            webPageOutput.println("</FONT></TD>");
            webPageOutput.print("<TD ALIGN=\"center\" WIDTH=\"43\"><FONT CLASS=\"lead\">" + divisionWins + "-" + divisionLosses);
            if (divisionDraws!=0)
               {
               webPageOutput.print("-" + divisionDraws);
               }
            webPageOutput.println("</FONT></TD>");
            webPageOutput.print("<TD ALIGN=\"center\" WIDTH=\"43\"><FONT CLASS=\"lead\">" + conferenceWins + "-" + conferenceLosses);
            if (conferenceDraws!=0)
               {
               webPageOutput.print("-" + conferenceDraws);
               }
            webPageOutput.println("</FONT></TD>");
            webPageOutput.print("<TD ALIGN=\"center\" WIDTH=\"43\"><FONT CLASS=\"lead\">" + interWins + "-" + interLosses);
            if (interDraws!=0)
               {
               webPageOutput.print("-" + interDraws);
               }
            webPageOutput.println("</FONT></TD>");
            webPageOutput.print("<TD ALIGN=\"right\"> <FONT CLASS=\"lead\">");
            if (streak==0)
               {
               webPageOutput.print("--");
               }
            else
               {
               if (streak>0)
                  {
                  webPageOutput.print("W" + streak);
                  }
               else
                  {
                  webPageOutput.print("L" + (streak * -1));
                  }
               }
            webPageOutput.println("</FONT></TD>");
            webPageOutput.println("</TR>");
            }
         webPageOutput.println("</TBODY></TABLE>");
	 webPageOutput.println("<P></P>");
	 webPageOutput.println("<FONT CLASS=\"lead\">");
	 webPageOutput.println("x-clinched playoff berth<BR>");
 	 webPageOutput.println("y-clinched division title<BR>");
  	 webPageOutput.println("z-clinched first-round bye<BR>");
	 webPageOutput.println("*-clinched homefield advantage");
         webPageOutput.println("</FONT>");
         webPageOutput.println("</TD>");
         webPageOutput.println("</TR>");
         webPageOutput.println("</TBODY></TABLE>");
         }
      catch(SQLException error)
         {
		 Routines.writeToLog(servletName,"Database error retrieving standings : " + error,false,context);	
         }
      finally
         {
         pool.returnConnection(database);
         }
      Routines.WriteHTMLTail(request,response,webPageOutput);
      }
   }