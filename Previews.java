import java.io.*;
import java.sql.*;
import javax.servlet.http.*;
import javax.servlet.*;

public class Previews
   {
   private static String servletName="Previews";	
   
   static void createScreen(HttpServletRequest request,
                            HttpServletResponse response,
                            Connection  database,
                            PrintWriter webPageOutput,
	                        ServletContext context)
      {
      HttpSession session=request.getSession();
      session.setAttribute("redirect",request.getRequestURL() + "?" + request.getQueryString());
      int league=Routines.safeParseInt((String)session.getAttribute("league"));
      int season=Routines.safeParseInt((String)session.getAttribute("viewSeason"));
      int week=Routines.safeParseInt((String)session.getAttribute("viewWeek"));
      int standingsWeek=Routines.safeParseInt((String)session.getAttribute("week"));
      if(season==0)
        {
        season=Routines.safeParseInt((String)session.getAttribute("season"));
        }
      if(week==0)
        {
        week=Routines.safeParseInt((String)session.getAttribute("week"));
        }
      int fixturesWeek=week;
      if (fixturesWeek==0)
         {
         fixturesWeek++;
         }
      // Retrieve Preview information from DB
      try
         {
         Statement sql1 = database.createStatement();
         Statement sql2 = database.createStatement();
         ResultSet rs1 = sql1.executeQuery("SELECT Name,Wins,Losses,Draws,AwayTeam,FixtureNumber " +
                                           "FROM fixtures,teams,standings " +
                                           "WHERE teams.TeamNumber = AwayTeam " +
                                           "AND fixtures.LeagueNumber = " + league + " " +
                                           "AND fixtures.Season = " + season + " " +
                                           "AND fixtures.Week = " + fixturesWeek + " " +
                                           "AND standings.LeagueNumber = fixtures.LeagueNumber " +
                                           "AND standings.Season = fixtures.Season " +
                                           "AND standings.Week = " + standingsWeek + " " +
                                           "AND standings.TeamNumber = AwayTeam " +
                                           "ORDER BY FixtureNumber ASC");
         ResultSet rs2 = sql2.executeQuery("SELECT Name,Wins,Losses,Draws,HomeTeam " +
                                           "FROM fixtures,teams,standings " +
                                           "WHERE teams.TeamNumber = HomeTeam " +
                                           "AND fixtures.LeagueNumber = " + league + " " +
                                           "AND fixtures.Season = " + season + " " +
                                           "AND fixtures.Week = " + fixturesWeek + " " +
                                           "AND standings.LeagueNumber = fixtures.LeagueNumber " +
                                           "AND standings.Season = fixtures.Season " +
                                           "AND standings.Week = " + standingsWeek + " " +
                                           "AND standings.TeamNumber = HomeTeam " +
                                           "ORDER BY FixtureNumber ASC");
         // Prepare screen
         webPageOutput.println("<CENTER>");
         webPageOutput.println("<IMG SRC=\"../Images/Preview.gif\"" +
                               " WIDTH='120' HEIGHT='60' ALT='Previews'>");
         webPageOutput.println("</CENTER>");
         Routines.titleHeader("head2",true,webPageOutput);
         int previewWeek=week;
         if(previewWeek==0)
           {
           previewWeek++;
           }
         webPageOutput.println("Previews for Season " +
                               season +
                               ", " +
                               Routines.decodeWeekNumber(Routines.safeParseInt((String)session.getAttribute("preSeasonWeeks")),
                                                                Routines.safeParseInt((String)session.getAttribute("regularSeasonWeeks")),
                                                                Routines.safeParseInt((String)session.getAttribute("postSeasonWeeks")),
                                                                season,
                                                                previewWeek,
                                                                false,
                                                                session));
         Routines.titleTrailer(true,webPageOutput);
         webPageOutput.println("<DIV CLASS=\"SLTables1\"><TABLE WIDTH=\"100%\" BORDER=\"0\">");
         webPageOutput.println("<TBODY>");
         int lineBreaker = 1;
         // Print preview of one particular game.
         // rs1 contains the awayTeams.
         // rs2 contains the homeTeams.
         // lineBreaker is used to print games per line.
         while(rs1.next())
            {
            rs2.next();
            lineBreaker++;
            if (lineBreaker == 1)
               {
               webPageOutput.println("<TD WIDTH=\"2%\"></TD>");
               webPageOutput.println("<TD WIDTH=\"49%\">");
               }
            if (lineBreaker == 2)
               {
               webPageOutput.println("<TR VALIGN=\"top\">");
               webPageOutput.println("<TD WIDTH=\"49%\">");
               }
            String awayTeamName   = new String(rs1.getString(1));
            String awayTeamInits  = new String(Routines.getInitials(awayTeamName));
            int    awayTeamWins   = rs1.getInt(2);
            int    awayTeamLosses = rs1.getInt(3);
            int    awayTeamDraws  = rs1.getInt(4);
            int    awayTeamNumber = rs1.getInt(5);
            int    fixtureNumber  = rs1.getInt(6);
            String homeTeamName   = new String(rs2.getString(1));
            String homeTeamInits  = new String(Routines.getInitials(homeTeamName));
            int    homeTeamWins   = rs2.getInt(2);
            int    homeTeamLosses = rs2.getInt(3);
            int    homeTeamDraws  = rs2.getInt(4);
            int    homeTeamNumber = rs2.getInt(5);
            webPageOutput.println("<TABLE WIDTH=\"100%\" CELLPADDING=\"1\" CELLSPACING=\"1\" BORDER=\"0\">");
            webPageOutput.println("<TBODY>");
            webPageOutput.println("<TR CLASS=\"columnrow\" ALIGN=\"center\">");
            webPageOutput.println("<TH WIDTH=\"50%\" ALIGN=\"left\">");
            webPageOutput.println("@ " + homeTeamName);
            webPageOutput.println("</TH>");
            if (awayTeamDraws == 0 && homeTeamDraws == 0)
               {
               webPageOutput.println("<TH>W-L");
               }
            else
               {
               webPageOutput.println("<TH>W-L-T");
               }
            webPageOutput.println("</TH>");
            webPageOutput.println("</TR>");
            webPageOutput.println("<TR ALIGN=\"center\" CLASS=\"scoresrow\">");
            webPageOutput.println("<TD ALIGN=\"left\" HEIGHT=\"20\"><B>");
            Routines.WriteHTMLLink(request,
                                   response,
                                   webPageOutput,
                                   "TeamNews",
                                   "league=" +
                                   league +
                                   "&team=" +
                                   awayTeamNumber,
                                   awayTeamName,
                                   null,
                                   true);
            webPageOutput.println("</B></TD>");
            webPageOutput.println("<TD>");
            webPageOutput.print(awayTeamWins +
                                "-" +
                                awayTeamLosses);
            if (awayTeamDraws!=0 || homeTeamDraws!=0)
               {
               webPageOutput.print("-" +
                                   awayTeamDraws);
               }
            webPageOutput.println("</TD>");
            webPageOutput.println("</TR>");
            webPageOutput.println("<TR ALIGN=\"center\" CLASS=\"scoresrow\">");
            webPageOutput.println("<TD ALIGN=\"left\" HEIGHT=\"20\"><B>");
            Routines.WriteHTMLLink(request,
                                   response,
                                   webPageOutput,
                                   "TeamNews",
                                   "league=" +
                                   league +
                                   "&team=" +
                                   homeTeamNumber,
                                   homeTeamName,
                                   null,
                                   true);
            webPageOutput.println("</B></TD>");
            webPageOutput.println("<TD>");
            webPageOutput.print(homeTeamWins +
                                "-" +
                                homeTeamLosses);
            if (homeTeamDraws!=0 || awayTeamDraws!=0)
               {
               webPageOutput.print("-" +
                                   homeTeamDraws);
               }
            webPageOutput.println("</TD>");
            webPageOutput.println("</TR>");
            webPageOutput.println("</TBODY>");
            webPageOutput.println("</TABLE>");
            webPageOutput.println("<TABLE WIDTH=\"100%\" CELLPADDING=\"0\" CELLSPACING=\"1\" BORDER=\"0\">");
            webPageOutput.println("<TBODY>");
            webPageOutput.println("<TR CLASS=\"scoresrow\">");
            webPageOutput.println("<TD>");
            webPageOutput.println("<TABLE WIDTH=\"100%\" CELLPADDING=\"0\" CELLSPACING=\"1\" BORDER=\"0\">");
            webPageOutput.println("<TBODY>");
            webPageOutput.println("<TR CLASS=\"scoresrow\">");
            webPageOutput.println("<TD>Rosters:");
            Routines.WriteHTMLLink(request,
                                          response,
                                          webPageOutput,
                                          "Roster",
                                          "league=" +
                                          league +
                                          "&season=" +
                                          season +
                                          "&week=" +
                                          week +
                                          "&team=" +
                                          awayTeamNumber,
                                          awayTeamInits,
                                          null,
                                          true);
            webPageOutput.println("|");
            Routines.WriteHTMLLink(request,
                                          response,
                                          webPageOutput,
                                          "Roster",
                                          "league=" +
                                          league +
                                          "&season=" +
                                          season +
                                          "&week=" +
                                          week +
                                          "&team=" +
                                          homeTeamNumber,
                                          homeTeamInits,
                                          null,
                                          true);
            webPageOutput.println("<BR>");
            webPageOutput.println("Depth Charts:");
            Routines.WriteHTMLLink(request,
                                          response,
                                          webPageOutput,
                                          "DepthChart",
                                          "league=" +
                                          league +
                                          "&season=" +
                                          season +
                                          "&week=" +
                                          week +
                                          "&team=" +
                                          awayTeamNumber,
                                          awayTeamInits,
                                          null,
                                          true);
            webPageOutput.println("|");
            Routines.WriteHTMLLink(request,
                                          response,
                                          webPageOutput,
                                          "DepthChart",
                                          "league=" +
                                          league +
                                          "&season=" +
                                          season +
                                          "&week=" +
                                          week +
                                          "&team=" +
                                          homeTeamNumber,
                                          homeTeamInits,
                                          null,
                                          true);
            webPageOutput.println("<BR>");
            webPageOutput.println("GameCenter:");
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
                                          "Preview",
                                          null,
                                          true);
            webPageOutput.println("</TD>");
            webPageOutput.println("</TR>");
            webPageOutput.println("</TBODY>");
            webPageOutput.println("</TABLE>");
            webPageOutput.println("</TD>");
            webPageOutput.println("</TR>");
            webPageOutput.println("</TBODY></TABLE>");
            webPageOutput.println("</TD>");
            if (lineBreaker == 2)
               {
               lineBreaker = 0;
               }
            }
         webPageOutput.println("</TBODY></TABLE></DIV>");
         webPageOutput.println(Routines.spaceLines(1));
         Routines.WriteHTMLTail(request,response,webPageOutput);
         }
      catch(SQLException error)
         {
		 Routines.writeToLog(servletName,"Database error retrieving previews : " + error,false,context);	
         }
      }
   }