import java.io.*;
import java.sql.*;
import javax.servlet.http.*;
import javax.servlet.*;

public class Results
   {
   private static String servletName="Results";
		
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
      if(season==0)
        {
        season=Routines.safeParseInt((String)session.getAttribute("season"));
        }

      if(week==0)
        {
        week=Routines.safeParseInt((String)session.getAttribute("week"));
        }
      // Retrieve Score information from DB
      try
         {
         Statement sql1 = database.createStatement();
         Statement sql2 = database.createStatement();
         Statement sql3 = database.createStatement();
         ResultSet queryResponse1;
         ResultSet queryResponse2;
         ResultSet queryResponse3;
         queryResponse1 = sql1.executeQuery("SELECT Name,AwayTeamScore,FinalQuarter," +
                                            "FixtureNumber,teams.TeamNumber," +
                                            "Wins,Losses,Draws," +
                                            "AwayTeamScore1,AwayTeamScore2," +
                                            "AwayTeamScore3,AwayTeamScore4," +
                                            "AwayTeamScore5," +
                                            "Surname,Forname,AwayTeamBestPasserAttempts," +
                                            "AwayTeamBestPasserCompletions,AwayTeamBestPasserYards," +
                                            "AwayTeamBestRunRec,AwayTeamBestRunRecAttempts,AwayTeamBestRunRecYards " +
                                            "FROM teams,fixtures,standings,players " +
                                            "WHERE teams.TeamNumber = AwayTeam " +
                                            "AND fixtures.LeagueNumber = " + league + " " +
                                            "AND fixtures.Season = " + season + " " +
                                            "AND fixtures.Week = " + week + " " +
                                            "AND standings.LeagueNumber = fixtures.LeagueNumber " +
                                            "AND standings.Season = fixtures.Season " +
                                            "AND standings.Week = fixtures.Week " +
                                            "AND standings.TeamNumber = AwayTeam " +
                                            "AND AwayTeamBestPasser = PlayerNumber " +
                                            "ORDER BY FixtureNumber ASC");
         queryResponse2 = sql2.executeQuery("SELECT Name,HomeTeamScore,teams.TeamNumber," +
                                            "Wins,Losses,Draws," +
                                            "HomeTeamScore1,HomeTeamScore2," +
                                            "HomeTeamScore3,HomeTeamScore4," +
                                            "HomeTeamScore5," +
                                            "Surname,ForName,HomeTeamBestPasserAttempts," +
                                            "HomeTeamBestPasserCompletions,HomeTeamBestPasserYards," +
                                            "HomeTeamBestRunRec,HomeTeamBestRunRecAttempts,HomeTeamBestRunRecYards " +
                                            "FROM teams,fixtures,standings,players " +
                                            "WHERE teams.TeamNumber = HomeTeam " +
                                            "AND fixtures.LeagueNumber = " + league + " " +
                                            "AND fixtures.Season = " + season + " " +
                                            "AND fixtures.Week = " + week + " " +
                                            "AND standings.LeagueNumber = fixtures.LeagueNumber " +
                                            "AND standings.Season = fixtures.Season " +
                                            "AND standings.Week = fixtures.Week " +
                                            "AND standings.TeamNumber = HomeTeam " +
                                            "AND HomeTeamBestPasser = PlayerNumber " +
                                            "ORDER BY FixtureNumber ASC");
         // Prepare screen
         Routines.titleHeader("head2",true,webPageOutput);
         webPageOutput.println("Scores for Season " +
                               season +
                               ", " +
                               Routines.decodeWeekNumber(Routines.safeParseInt((String)session.getAttribute("preSeasonWeeks")),
                                                                Routines.safeParseInt((String)session.getAttribute("regularSeasonWeeks")),
                                                                Routines.safeParseInt((String)session.getAttribute("postSeasonWeeks")),
                                                                season,
                                                                week,
                                                                false,
                                                                session));
         Routines.titleTrailer(true,webPageOutput);
         webPageOutput.println("<DIV CLASS=\"SLTables1\"><TABLE WIDTH=\"100%\" BORDER=\"0\">");
         webPageOutput.println("<TBODY>");
         int lineBreaker = 1;
         // Print scores of one particular game.
         // queryResponse1 contains the awayTeams.
         // queryResponse2 contains the homeTeams.
         // lineBreaker is used to print games per line.
         while(queryResponse1.next())
            {
            queryResponse2.next();
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
            // Load all DB values into meaningfull names.
            String awayTeamName   = new String(queryResponse1.getString(1));
            String awayTeamInits  = new String(Routines.getInitials(awayTeamName));
            int    awayTeamScore  = queryResponse1.getInt(2);
            int    finalQuarter   = queryResponse1.getInt(3);
            int    fixtureNumber  = queryResponse1.getInt(4);
            int    awayTeamNumber = queryResponse1.getInt(5);
            int    awayTeamWins   = queryResponse1.getInt(6);
            int    awayTeamLosses = queryResponse1.getInt(7);
            int    awayTeamDraws  = queryResponse1.getInt(8);
            int    awayTeamScores[] = {queryResponse1.getInt(9),
                                       queryResponse1.getInt(10),
                                       queryResponse1.getInt(11),
                                       queryResponse1.getInt(12),
                                       queryResponse1.getInt(13)};
            String awayTeamBestPasserSurname     = queryResponse1.getString(14);
            String awayTeamBestPasserForname     = queryResponse1.getString(15);
            int    awayTeamBestPasserAttempts    = queryResponse1.getInt(16);
            int    awayTeamBestPasserCompletions = queryResponse1.getInt(17);
            int    awayTeamBestPasserYards       = queryResponse1.getInt(18);
            int    awayTeamBestRunRec            = queryResponse1.getInt(19);
            int    awayTeamBestRunRecAttempts    = queryResponse1.getInt(20);
            int    awayTeamBestRunRecYards       = queryResponse1.getInt(21);
            int    quarterPercent = 50 / (finalQuarter + 1);
            String homeTeamName   = new String(queryResponse2.getString(1));
            String homeTeamInits  = new String(Routines.getInitials(homeTeamName));
            int    homeTeamScore  = queryResponse2.getInt(2);
            int    homeTeamNumber = queryResponse2.getInt(3);
            int    homeTeamWins   = queryResponse2.getInt(4);
            int    homeTeamLosses = queryResponse2.getInt(5);
            int    homeTeamDraws  = queryResponse2.getInt(6);
            int    homeTeamScores[] = {queryResponse2.getInt(7),
                                       queryResponse2.getInt(8),
                                       queryResponse2.getInt(9),
                                       queryResponse2.getInt(10),
                                       queryResponse2.getInt(11)};
            String homeTeamBestPasserSurname     = queryResponse2.getString(12);
            String homeTeamBestPasserForname     = queryResponse2.getString(13);
            int    homeTeamBestPasserAttempts    = queryResponse2.getInt(14);
            int    homeTeamBestPasserCompletions = queryResponse2.getInt(15);
            int    homeTeamBestPasserYards       = queryResponse2.getInt(16);
            int    homeTeamBestRunRec            = queryResponse2.getInt(17);
            int    homeTeamBestRunRecAttempts    = queryResponse2.getInt(18);
            int    homeTeamBestRunRecYards       = queryResponse2.getInt(19);
            // Retrieve name of best runner/receiver.
            //    For the home team.
            queryResponse3 = sql3.executeQuery("SELECT Surname,Forname " +
                                               "FROM   players " +
                                               "WHERE  PlayerNumber = " + homeTeamBestRunRec);
            queryResponse3.first();
            String homeTeamBestRunRecSurname     = queryResponse3.getString(1);
            String homeTeamBestRunRecForname     = queryResponse3.getString(2);
            //   For the away team.
            queryResponse3 = sql3.executeQuery("SELECT Surname,Forname " +
                                               "FROM   players " +
                                               "WHERE  PlayerNumber = " + awayTeamBestRunRec);
            queryResponse3.first();
            String awayTeamBestRunRecSurname     = queryResponse3.getString(1);
            String awayTeamBestRunRecForname     = queryResponse3.getString(2);
            // Print Scores Table
            webPageOutput.println("<TABLE WIDTH=\"100%\" CELLPADDING=\"1\" CELLSPACING=\"1\" BORDER=\"0\">");
            webPageOutput.println("<TBODY>");
            webPageOutput.println("<TR CLASS=\"columnrow\" ALIGN=\"center\">");
            webPageOutput.println("<TH WIDTH=\"50%\" ALIGN=\"left\">");
            webPageOutput.println("Final");
            webPageOutput.println("</TH>");
            int width = 0;
            // Define Width of Scores Fields depending on how many quarters played.
            if (finalQuarter == 4)
               {
               width = 10;
               }
            else
               {
               width = 8;
               }
            // For each quarter, print a header Field.
            for (int currentQuarter=1;currentQuarter<=finalQuarter;currentQuarter++)
               {
               webPageOutput.print("<TD WIDTH=\"" + width + "%\">");
               if (currentQuarter<5)
                  {
                  webPageOutput.println(currentQuarter + "</TD>");
                  }
               else
                  {
                  webPageOutput.println("OT</TD>");
                  }
               }
            // And the final score.
            webPageOutput.print("<TH WIDTH=\"" + width + "%\">T</TH>");
            webPageOutput.println("</TR>");
            // Print away team table entry.
            webPageOutput.println("<TR ALIGN=\"center\" CLASS=\"scoresrow\">");
            webPageOutput.println("<TD ALIGN=\"left\" HEIGHT=\"20\"><B>");
            Routines.WriteHTMLLink(request,
                                          response,
                                          webPageOutput,
                                          "wfafl",
                                          "action=viewTeam" +
                                          "&value=" +
                                          awayTeamNumber +
                                          "&league=" +
                                          league +
                                          "&season=" +
                                          season +
                                          "&week=" +
                                          week,
                                          awayTeamName,
                                          null,
                                          true);
            webPageOutput.println("</B>");
            // Print away Team record.
            webPageOutput.print(" (" +
                                awayTeamWins +
                                "-" +
                                awayTeamLosses);
            if (awayTeamDraws!=0 || homeTeamDraws!=0)
               {
               webPageOutput.print("-" +
                                   awayTeamDraws);
               }
            webPageOutput.println(")");
            if (awayTeamScore > homeTeamScore)
               {
               webPageOutput.print("<FONT CLASS=\"winarrow\">«</FONT>");
               }
            webPageOutput.println("</TD>");
            // Print away team scoring by quarter.
            for (int currentQuarter=0;currentQuarter<finalQuarter;currentQuarter++)
                {
                webPageOutput.print("<TD>");
                webPageOutput.print(awayTeamScores[currentQuarter]);
                webPageOutput.println("</TD>");
                }
            // And the final score.
            webPageOutput.println("<TD CLASS=\"finalScore\">" +
                                  awayTeamScore +
                                  "</TD>");
            webPageOutput.println("</TR>");

            // Print home team table entry.
            webPageOutput.println("<TR ALIGN=\"center\" CLASS=\"scoresrow\">");
            webPageOutput.println("<TD ALIGN=\"left\" HEIGHT=\"20\"><B>");
            Routines.WriteHTMLLink(request,
                                          response,
                                          webPageOutput,
                                          "wfafl",
                                          "action=viewTeam" +
                                          "&value=" +
                                          homeTeamNumber +
                                          "&league=" +
                                          league +
                                          "&season=" +
                                          season +
                                          "&week=" +
                                          week,
                                          homeTeamName,
                                          null,
                                          true);
            webPageOutput.println("</B>");
            // Print home team record.
            webPageOutput.print(" (" +
                                homeTeamWins +
                                "-" +
                                homeTeamLosses);
            if (awayTeamDraws!=0 || homeTeamDraws!=0)
               {
               webPageOutput.print("-" +
                                   homeTeamDraws);
               }
            webPageOutput.println(")");
            if (homeTeamScore > awayTeamScore)
               {
               webPageOutput.print("<FONT CLASS=\"winarrow\">«</FONT>");
               }
            webPageOutput.println("</TD>");
            // Print home team scoring by quarter.
            for (int currentQuarter=0;currentQuarter<finalQuarter;currentQuarter++)
                {
                webPageOutput.print("<TD>");
                webPageOutput.print(homeTeamScores[currentQuarter]);
                webPageOutput.println("</TD>");
                }
            // And the final score.
            webPageOutput.println("<TD CLASS=\"finalScore\">" +
                                  homeTeamScore +
                                  "</TD>");
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
            webPageOutput.println("<TD>");
            // Print best performers details.
            // For the away team.
            webPageOutput.println("<B>");
            webPageOutput.println(awayTeamInits + ":");
            webPageOutput.println("</B>");
            webPageOutput.println(" " +
                                  awayTeamBestPasserForname.substring(0,1) +
                                  ". " +
                                  awayTeamBestPasserSurname +
                                  " (" +
                                  awayTeamBestPasserAttempts +
                                  "-" +
                                  awayTeamBestPasserCompletions +
                                  ", " +
                                  awayTeamBestPasserYards +
                                  ")");
            webPageOutput.println(", " +
                                  awayTeamBestRunRecForname.substring(0,1) +
                                  ". " +
                                  awayTeamBestRunRecSurname +
                                  " (" +
                                  awayTeamBestRunRecAttempts +
                                  "-" +
                                  awayTeamBestRunRecYards +
                                  ")");
            // And the home team.
            webPageOutput.println("<BR>");
            webPageOutput.println("<B>");
            webPageOutput.println(homeTeamInits + ":");
            webPageOutput.println("</B>");
            webPageOutput.println(" " +
                                  homeTeamBestPasserForname.substring(0,1) +
                                  ". " +
                                  homeTeamBestPasserSurname +
                                  " (" +
                                  homeTeamBestPasserAttempts +
                                  "-" +
                                  homeTeamBestPasserCompletions +
                                  ", " +
                                  homeTeamBestPasserYards +
                                  ")");
            webPageOutput.println(", " +
                                  homeTeamBestRunRecForname.substring(0,1) +
                                  ". " +
                                  homeTeamBestRunRecSurname +
                                  " (" +
                                  homeTeamBestRunRecAttempts +
                                  "-" +
                                  homeTeamBestRunRecYards +
                                  ")");
            webPageOutput.println("</TD>");
            webPageOutput.println("</TR>");
            webPageOutput.println("</TBODY>");
            webPageOutput.println("</TABLE>");
            webPageOutput.println("</TD>");
            webPageOutput.println("</TR>");
            // Various Sub Screens.
            webPageOutput.println("<TR CLASS=\"scoresrow\">");
            webPageOutput.println("<TD>");
            webPageOutput.println("GameCenter:");
            Routines.WriteHTMLLink(request,
                                          response,
                                          webPageOutput,
                                          "wfafl",
                                          "action=viewRecap" +
                                          "&value=" +
                                          fixtureNumber +
                                          "&league=" +
                                          league +
                                          "&season=" +
                                          season +
                                          "&week=" +
                                          week,
                                          "Recap",
                                          null,
                                          true);
            webPageOutput.println("<B>·</B>");
            Routines.WriteHTMLLink(request,
                                          response,
                                          webPageOutput,
                                          "wfafl",
                                          "action=viewGameStats" +
                                          "&value=" +
                                          fixtureNumber +
                                          "&league=" +
                                          league +
                                          "&season=" +
                                          season +
                                          "&week=" +
                                          week,
                                          "Game Stats",
                                          null,
                                          true);
            webPageOutput.println("<B>·</B>");
            Routines.WriteHTMLLink(request,
                                          response,
                                          webPageOutput,
                                          "PlayByPlay",
                                          "league=" +
                                          league +
                                          "&season=" +
                                          season +
                                          "&week=" +
                                          week +
                                          "&fixture=" +
                                          fixtureNumber +
                                          "&value2=0",
                                          "Play by Play",
                                          null,
                                          true);
            webPageOutput.println("<BR>");
            webPageOutput.println("<FONT CLASS=\"wordspacer\">");
            webPageOutput.println("GameCenter:");
            webPageOutput.println("</FONT>");
            Routines.WriteHTMLLink(request,
                                          response,
                                          webPageOutput,
                                          "wfafl",
                                          "action=viewGameBook" +
                                          "&value=" +
                                          fixtureNumber +
                                          "&league=" +
                                          league +
                                          "&season=" +
                                          season +
                                          "&week=" +
                                          week,
                                          "Drive Charts",
                                          null,
                                          true);
            webPageOutput.println("<B>·</B>");
            Routines.WriteHTMLLink(request,
                                          response,
                                          webPageOutput,
                                          "wfafl",
                                          "action=viewGameBook" +
                                          "&value=" +
                                          fixtureNumber +
                                          "&league=" +
                                          league +
                                          "&season=" +
                                          season +
                                          "&week=" +
                                          week,
                                          "Gamebook",
                                          null,
                                          true);
            webPageOutput.println("<B>·</B>");
            Routines.WriteHTMLLink(request,
                                          response,
                                          webPageOutput,
                                          "wfafl",
                                          "action=viewGameViewer" +
                                          "&value=" +
                                          fixtureNumber +
                                          "&league=" +
                                          league +
                                          "&season=" +
                                          season +
                                          "&week=" +
                                          week,
                                          "Game Viewer",
                                          null,
                                          true);
            webPageOutput.println("</TD>");
            webPageOutput.println("</TR>");
            webPageOutput.println("<TR>");
            webPageOutput.println("<TD HEIGHT=\"10\"></TD>");
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
         Routines.writeToLog(servletName,"Database error retrieving scores : " + error,false,context);	
         }
      }
   }




//      if ("viewAllScores".equals(subAction))
//         {
//         currentLeague.viewAllScores();
//	 }
//      if (currentLeague.allScoresRevealed() == false)
//         {
//         webPageOutput.println("<BR>");
//         Routines.WriteHTMLLink(webPageOutput,
//                                       currentSession.getScreen() +
//                                       "?action=viewLeagueGames&subAction=viewAllScores&sessionNumber="
//                                       + currentSession.getSessionNumber(),
//                                       "Reveal Scores",null);
//	 webPageOutput.println("<BR><BR>");
//         }
//      for (int currentGame=0; currentGame<currentLeague.getNumOfGames(); currentGame++)
//          {
//          webPageOutput.println("<TR>");
//          webPageOutput.println("<TD WIDTH=234>" +
//                                currentLeague.games[currentGame].getAwayTeamName() +
//                                "</TD>");
//          if (currentLeague.games[currentGame].getViewScore() == false)
//	     {
//             webPageOutput.println("<TD WIDTH=018><P ALIGN=CENTER>");
//             Routines.WriteHTMLLink(webPageOutput,
//                                           currentSession.getScreen() +
//                                           "?action=viewLeagueGames&subAction=viewScore" +
//                                           currentGame +
//                                           "&sessionNumber=" +
//                                           currentSession.getSessionNumber(),
//                                           "**",null );
//	     webPageOutput.println("</TD>");
//	     }
  //        else
//	     {
  //           webPageOutput.println("<TD WIDTH=018><P ALIGN=RIGHT>" + currentLeague.games[currentGame].getAwayTeamScore() + "</TD>");
//	     }
  //        webPageOutput.println("<TD WIDTH=012>at</TD>");
    //      webPageOutput.println("<TD WIDTH=234>" +
//                                currentLeague.games[currentGame].getHomeTeamName() +
//                                "</TD>")/;
//	  if (currentLeague.games[currentGame].getViewScore() == false)
//	     {
//             webPageOutput.println("<TD WIDTH=018><P ALIGN=CENTER>");
//             Routines.WriteHTMLLink(webPageOutput,
//                                           currentSession.getScreen() +
//                                           "?action=viewLeagueResults&subAction=viewScore" +
//                                           currentGame +
//                                           "&sessionNumber=" +
//                                           currentSession.getSessionNumber(),
//                                           "**",null);
//	     webPageOutput.println("</TD>");
//	     }
//          else
//	     {
//             webPageOutput.println("<TD WIDTH=018><P ALIGN=RIGHT>" +
//                                   currentLeague.games[currentGame].getHomeTeamScore() +
//                                   "</TD>")/;
//	     }
//          webPageOutput.println("<TD WIDTH=060>GameViewer</TD>");
//          webPageOutput.println("<TD WIDTH=075>Play by Play</TD>");
//          webPageOutput.println("<TD WIDTH=054>Box Score</TD>");
//          webPageOutput.println("</TR>");
//          }
//   public void viewAllScores()
//      {
// /     for ( int currentGame=0; currentGame<numOfGames; currentGame++ )
//          {/
// 	  this.games[currentGame].setViewScore();
//          }
//      }

//   public boolean allScoresRevealed()
//      {
//      boolean allScoresRevealed = true;
//      for ( int currentGame=0; currentGame<numOfGames; currentGame++ )
//         {
//	 if ( this.games[currentGame].getViewScore() == false )
//	    {
//	    allScoresRevealed = false;
//	    }
//	 }
  //    return allScoresRevealed;
//      }