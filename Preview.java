import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class Preview extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="Preview";

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
      Routines.WriteHTMLHead("Preview",//title
                             true,//showMenu
                             2,//menuHighLight
                             true,//seasonsMenu
		                     false,//weeksMenu
                             true,//scores
                             false,//standings
                             false,//gameCenter
                             false,//schedules
                             true,//previews
                             false,//teamCenter
		                     false,//draft
                             database,//database
                             request,//request
                             response,//response
                             webPageOutput,//webPageOutput
                             context);//context
      int season=Routines.safeParseInt((String)session.getAttribute("viewSeason"));
      int week=Routines.safeParseInt((String)session.getAttribute("viewWeek"));
      int latestSeason=Routines.safeParseInt((String)session.getAttribute("season"));
      int latestWeek=Routines.safeParseInt((String)session.getAttribute("week"));
      int homeTeamNumber=Routines.safeParseInt((String)session.getAttribute("homeTeamNumber"));
      int awayTeamNumber=Routines.safeParseInt((String)session.getAttribute("awayTeamNumber"));
      boolean success=true;
      String errorMessage="";
      String[] teamNames=new String[2];
      teamNames[0]=(String)session.getAttribute("awayTeamName");
      teamNames[1]=(String)session.getAttribute("homeTeamName");
      int[] teamNumbers={0,0};
      int[] conferenceNumbers={0,0};
      String[] conferenceNames={"",""};
      int[] divisionNumbers={0,0};
      String[] divisionNames={"",""};
      int[] wins={0,0};
      int[] losses={0,0};
      int[] draws={0,0};
      int[] scored={0,0};
      int[] conceded={0,0};
      int[] homeWins={0,0};
      int[] homeLosses={0,0};
      int[] homeDraws={0,0};
      int[] awayWins={0,0};
      int[] awayLosses={0,0};
      int[] awayDraws={0,0};
      int[] divisionWins={0,0};
      int[] divisionLosses={0,0};
      int[] divisionDraws={0,0};
      int[] conferenceWins={0,0};
      int[] conferenceLosses={0,0};
      int[] conferenceDraws={0,0};
      int[] interWins={0,0};
      int[] interLosses={0,0};
      int[] interDraws={0,0};
      String[] forNames={"",""};
      String[] surNames={"",""};
      int[] coachNumbers={0,0};
      int[] streak={0,0};
      int[] position={0,0};
      String titleText="";
      int[] fixtureWins={0,0};
      int[] fixtureLosses={0,0};
      int[] fixtureDraws={0,0};

      int[][] injuredPlayers=new int[50][50];
      String[][] injuredPositions=new String[50][50];
      String[][] injuredNames=new String[50][50];
      int[][] injuredWeeks=new int[50][50];

      int standingsWeek=latestWeek;
      if(latestSeason==season&&latestWeek==0)
         {
         standingsWeek=0;
         }
      try
         {
         Statement sql = database.createStatement();
         ResultSet queryResponse;
         queryResponse = sql.executeQuery("SELECT teams.TeamNumber," +
                                          "conferences.ConferenceNumber," +
                                          "conferences.ConferenceName," +
                                          "divisions.DivisionNumber," +
                                          "divisions.DivisionName," +
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
                                          "Streak " +
                                          "FROM standings," +
                                          "teams," +
                                          "leagueteams," +
                                          "divisions," +
                                          "conferences," +
                                          "coaches " +
                                          "WHERE teams.TeamNumber           = standings.TeamNumber " +
                                          "AND   teams.TeamNumber           = leagueteams.TeamNumber " +
                                          "AND   leagueteams.DivisionNumber = divisions.DivisionNumber " +
                                          "AND   divisions.ConferenceNumber = conferences.ConferenceNumber " +
                                          "AND   conferences.LeagueNumber   = " + (String)session.getAttribute("league") + " " +
                                          "AND   standings.Season           = " + season + " " +
                                          "AND   standings.Week             = " + standingsWeek + " " +
                                          "AND   standings.CoachNumber      = coaches.CoachNumber " +
                                          "ORDER BY divisions.DivisionNumber ASC,Position ASC");
         int currentPosition=0;
         int currentDivision=0;
         while(queryResponse.next())
           {
           int teamNumber = queryResponse.getInt(1);
           int divisionNumber = queryResponse.getInt(4);
           int currentTeam=0;
           if(currentDivision==0||currentDivision!=divisionNumber)
             {
             currentDivision=divisionNumber;
             currentPosition=1;
             }
           else
             {
             currentPosition++;
             }
           if(teamNumber==homeTeamNumber || teamNumber==awayTeamNumber)
             {
             if(teamNumber==homeTeamNumber)
               {
               currentTeam=1;
               }
             else
               {
               currentTeam=0;
               }
             teamNumbers[currentTeam] = queryResponse.getInt(1);
             conferenceNumbers[currentTeam] = queryResponse.getInt(2);
             conferenceNames[currentTeam] = queryResponse.getString(3);
             divisionNumbers[currentTeam] = queryResponse.getInt(4);
             divisionNames[currentTeam] = queryResponse.getString(5);
             wins[currentTeam] = queryResponse.getInt(6);
             losses[currentTeam] = queryResponse.getInt(7);
             draws[currentTeam] = queryResponse.getInt(8);
             scored[currentTeam] = queryResponse.getInt(9);
             conceded[currentTeam] = queryResponse.getInt(10);
             homeWins[currentTeam] = queryResponse.getInt(11);
             homeLosses[currentTeam] = queryResponse.getInt(12);
             homeDraws[currentTeam] = queryResponse.getInt(13);
             awayWins[currentTeam] = queryResponse.getInt(14);
             awayLosses[currentTeam] = queryResponse.getInt(15);
             awayDraws[currentTeam] = queryResponse.getInt(16);
             divisionWins[currentTeam] = queryResponse.getInt(17);
             divisionLosses[currentTeam] = queryResponse.getInt(18);
             divisionDraws[currentTeam] = queryResponse.getInt(19);
             conferenceWins[currentTeam] = queryResponse.getInt(20);
             conferenceLosses[currentTeam] = queryResponse.getInt(21);
             conferenceDraws[currentTeam] = queryResponse.getInt(22);
             interWins[currentTeam] = queryResponse.getInt(23);
             interLosses[currentTeam] = queryResponse.getInt(24);
             interDraws[currentTeam] = queryResponse.getInt(25);
             forNames[currentTeam] = queryResponse.getString(26);
             surNames[currentTeam] = queryResponse.getString(27);
             coachNumbers[currentTeam] = queryResponse.getInt(28);
             streak[currentTeam] = queryResponse.getInt(29);
             position[currentTeam] = currentPosition;
             }
           }
         for(int currentTeam=0;currentTeam<2;currentTeam++)
            {
            queryResponse = sql.executeQuery("SELECT players.PlayerNumber,PositionCode,Surname,Forname,Injury,Doctors " +
                                             "FROM   players,positions " +
                                             "WHERE  TeamNumber = " + teamNumbers[currentTeam] + " " +
                                             "AND    Injury > 0 " +
                                             "AND    players.PositionNumber = positions.PositionNumber " +
                                             "ORDER  BY Injury ASC");
            int currentPlayer=-1;
            while(queryResponse.next())
               {
               currentPlayer++;
               injuredPlayers[currentTeam][currentPlayer]=queryResponse.getInt(1);
               injuredPositions[currentTeam][currentPlayer]=queryResponse.getString(2);
               injuredNames[currentTeam][currentPlayer]=queryResponse.getString(4) + " " + queryResponse.getString(3);
               if(queryResponse.getInt(6)==0)
                 {
                 injuredWeeks[currentTeam][currentPlayer]=queryResponse.getInt(5);
                 }
               else
                 {
                 if(queryResponse.getInt(5)>0)
                   {
                   injuredWeeks[currentTeam][currentPlayer]=queryResponse.getInt(5)+1;
                   }
                 else
                   {
                   injuredWeeks[currentTeam][currentPlayer]=queryResponse.getInt(5);
                   }
                 }
               }
            }
         }
      catch(SQLException error)
         {
		 Routines.writeToLog(servletName,"Database error retrieving standings : " + error,false,context);	
         }
      finally
         {
         pool.returnConnection(database);
         }

      if(success)
        {
        if(divisionNumbers[0]==divisionNumbers[1])
          {
          titleText="Division";
          for(int currentTeam=0;currentTeam<2;currentTeam++)
             {
             fixtureWins[currentTeam]=divisionWins[currentTeam];
             fixtureLosses[currentTeam]=divisionLosses[currentTeam];
             fixtureDraws[currentTeam]=divisionDraws[currentTeam];
             }
          }
        else
          {
          if(conferenceNumbers[0]==conferenceNumbers[1])
            {
            titleText="Conference";
            for(int currentTeam=0;currentTeam<2;currentTeam++)
               {
               fixtureWins[currentTeam]=conferenceWins[currentTeam];
               fixtureLosses[currentTeam]=conferenceLosses[currentTeam];
               fixtureDraws[currentTeam]=conferenceDraws[currentTeam];
               }
            }
          else
            {
            titleText="Inter";
            for(int currentTeam=0;currentTeam<2;currentTeam++)
               {
               fixtureWins[currentTeam]=interWins[currentTeam];
               fixtureLosses[currentTeam]=interLosses[currentTeam];
               fixtureDraws[currentTeam]=interDraws[currentTeam];
               }
            }
          }
       }
     if(success)
        {
        webPageOutput.println("<DIV CLASS=\"SLTables1\">");
        webPageOutput.println("<TABLE CELLSPACING=\"1\" CELLPADDING=\"1\" WIDTH=\"100%\" BORDER=\"0\">");
        webPageOutput.println("<TBODY>");
        webPageOutput.println("<TR CLASS=\"white\" BGCOLOR=\"#000a78\">");
        webPageOutput.println("<TD>STANDINGS</TD>");
        webPageOutput.println("</TR>");
        webPageOutput.println("</TBODY>");
        webPageOutput.println("</TABLE>");
        webPageOutput.println("<DIV CLASS=\"SLTables2\">");
        webPageOutput.println("<TABLE WIDTH=\"100%\" CELLPADDING=\"2\" CELLSPACING=\"1\" BORDER=\"0\">");
        webPageOutput.println("<TBODY>");
        webPageOutput.println("<TR ALIGN=\"center\" CLASS=\"bg1\">");
        webPageOutput.println("<TD><B>Team</B></TD>");
        webPageOutput.println("<TD><B>Coach</B></TD>");
        webPageOutput.println("<TD><B>Standing</B></TD>");
        webPageOutput.println("<TD><B>W</B></TD>");
        webPageOutput.println("<TD><B>L</B></TD>");
        webPageOutput.println("<TD><B>T</B></TD>");
        webPageOutput.println("<TD><B>PF</B></TD>");
        webPageOutput.println("<TD><B>PA</B></TD>");
        webPageOutput.println("<TD><B>Home or Away</B></TD>");
        webPageOutput.println("<TD><B>" + titleText + "</B></TD>");
        webPageOutput.println("<TD><B>Streak</B></TD>");
        webPageOutput.println("</TR>");
        for(int currentTeam=0;currentTeam<2;currentTeam++)
           {
           webPageOutput.println("<TR CLASS=\"bg2\">");
           webPageOutput.println("<TD ALIGN=\"left\">");
		   Routines.WriteHTMLLink(request,
								  response,
								  webPageOutput,
								  "TeamNews",
								  "league=" +
			                      (String)session.getAttribute("league") +
								  "&team=" +
			                      teamNumbers[currentTeam],
			                      teamNames[currentTeam],
								  null,
								  true);
//           Routines.WriteHTMLLink(request,
//                                         response,
//                                         webPageOutput,
//                                         "wfafl",
//                                         "action=viewTeam" +
//                                         "&value=" +
//                                         teamNumbers[currentTeam],
//                                         teamNames[currentTeam],
//                                         null,
//                                         true);
           webPageOutput.println("</TD>");
           webPageOutput.println("<TD>");
           String formattedName = new String(Routines.formatName(forNames[currentTeam],surNames[currentTeam],11));
           Routines.WriteHTMLLink(request,
                                         response,
                                         webPageOutput,
                                         "wfafl",
                                         "action=viewCoach" +
                                         "&value=" +
                                         coachNumbers[currentTeam],
                                         formattedName,
                                         null,
                                         true);
           webPageOutput.println("</TD>");
           if(latestWeek==0)
             {
             webPageOutput.println("<TD>" +
                                   conferenceNames[currentTeam].substring(0,1) +
                                   "FC " + divisionNames[currentTeam] +
                                   "</TD>");
             }
           else
             {
             webPageOutput.println("<TD>" +
                                   Routines.positionText(position[currentTeam],context)  +
                                   " " +
                                   conferenceNames[currentTeam].substring(0,1) +
                                   "FC " + divisionNames[currentTeam] +
                                   "</TD>");
             }
           webPageOutput.println("<TD ALIGN=\"right\">" + wins[currentTeam] + "</TD>");
           webPageOutput.println("<TD ALIGN=\"right\">" + losses[currentTeam] + "</TD>");
           webPageOutput.println("<TD ALIGN=\"right\">" + draws[currentTeam] + "</TD>");
           webPageOutput.println("<TD ALIGN=\"right\">" + scored[currentTeam] + "</TD>");
           webPageOutput.println("<TD ALIGN=\"right\">" + conceded[currentTeam] + "</TD>");
           String homeAwayTitle="";
           int homeAwayWins=0;
           int homeAwayLosses=0;
           int homeAwayDraws=0;
           if(currentTeam==0)
             {
             homeAwayTitle="<I>(Away) </I>";
             homeAwayWins=awayWins[currentTeam];
             homeAwayLosses=awayLosses[currentTeam];
             homeAwayDraws=awayDraws[currentTeam];
             }
           else
             {
             homeAwayTitle="<I>(Home) </I>";
             homeAwayWins=homeWins[currentTeam];
             homeAwayLosses=homeLosses[currentTeam];
             homeAwayDraws=homeDraws[currentTeam];
             }
           webPageOutput.print("<TD ALIGN=\"right\">");
           webPageOutput.print(homeAwayTitle + homeAwayWins + "-" + homeAwayLosses);
           if(homeAwayDraws!=0)
             {
             webPageOutput.print("-" + homeAwayDraws);
             }
           webPageOutput.println("</TD>");
           webPageOutput.print("<TD ALIGN=\"right\">");
           webPageOutput.print(fixtureWins[currentTeam] + "-" + fixtureLosses[currentTeam]);
           if(fixtureDraws[currentTeam]!=0)
             {
             webPageOutput.print("-" + fixtureDraws[currentTeam]);
             }
           webPageOutput.println("</TD>");
           webPageOutput.print("<TD ALIGN=\"right\">");
            if (streak[currentTeam]==0)
               {
               webPageOutput.print("--");
               }
            else
               {
               if (streak[currentTeam]>0)
                  {
                  webPageOutput.print("W" + streak[currentTeam]);
                  }
               else
                  {
                  webPageOutput.print("L" + (streak[currentTeam] * -1));
                  }
               }
           webPageOutput.println("</TD>");
           webPageOutput.println("</TR>");
           }
        webPageOutput.println("</TBODY>");
        webPageOutput.println("</TABLE>");
        webPageOutput.println("</DIV>");
        //Injury Tables
        webPageOutput.println("<BR>");
        for(int currentTeam=0;currentTeam<2;currentTeam++)
           {
           webPageOutput.println("<DIV CLASS=\"SLTables1\">");
           webPageOutput.println("<TABLE WIDTH=\"100%\" CELLPADDING=\"2\" CELLSPACING=\"1\" BORDER=\"0\">");
           webPageOutput.println("<TBODY>");
           String colour="";
           if(currentTeam==0)
             {
             colour="away";
             }
           else
             {
             colour="home";
             }
           webPageOutput.println("<TR CLASS=\"" + colour + "\">");
           webPageOutput.println("<TD CLASS=\"" + colour + "\"><B>" + teamNames[currentTeam] + " Injuries</B></TD></TR>");
           webPageOutput.println("</TBODY>");
           webPageOutput.println("</TABLE>");
           webPageOutput.println("<TABLE WIDTH=\"100%\" CELLPADDING=\"2\" CELLSPACING=\"1\" BORDER=\"0\">");
           webPageOutput.println("<TBODY>");
           webPageOutput.println("<TR VALIGN=\"top\" CLASS=\"bg2\">");
           int nextStartingPoint=0;
           boolean title=false;
           for(int currentPlayer=0;currentPlayer<injuredPlayers.length;currentPlayer++)
              {
              int tempInjury=Routines.adjustInjuryLength(injuredWeeks[currentTeam][currentPlayer],
                                                                latestWeek,
                                                                week);
              if(tempInjury==1&&title==false)
                {
                webPageOutput.println("<TD WIDTH=\"20%\">QUESTIONABLE</TD>");
                webPageOutput.println("<TD>");
                title=true;
                }
              if(tempInjury==1)
                {
                webPageOutput.println(injuredPositions[currentTeam][currentPlayer] +
                                      " " +
                                      injuredNames[currentTeam][currentPlayer] +
                                      " <BR>");
                }
              if(tempInjury>1)
                {
                nextStartingPoint=currentPlayer;
                currentPlayer=injuredPlayers.length;
                }
              }
           for(int currentPlayer=nextStartingPoint;currentPlayer<injuredPlayers.length;currentPlayer++)
              {
              int tempInjury=Routines.adjustInjuryLength(injuredWeeks[currentTeam][currentPlayer],
                                                                latestWeek,
                                                                week);
              if(currentPlayer==nextStartingPoint)
                {
                if(tempInjury>1)
                  {
                  webPageOutput.println("</TD>");
                  webPageOutput.println("</TR>");
                  webPageOutput.println("<TR VALIGN=\"top\" CLASS=\"bg2\">");
                  webPageOutput.println("<TD WIDTH=\"20%\">OUT</TD>");
                  webPageOutput.println("<TD>");
                  title=true;
                  }
                }
              if(tempInjury<1)
                {
                currentPlayer=injuredPlayers.length;
                }
              else
                {
                webPageOutput.println(injuredPositions[currentTeam][currentPlayer] +
                                      " " +
                                      injuredNames[currentTeam][currentPlayer] +
                                      " <BR>");
                }
              }
              if(title==false)
                {
                webPageOutput.println("<TD>");
                webPageOutput.println("None <BR>");
                }
           webPageOutput.println("</TD>");
           webPageOutput.println("</TR>");
           webPageOutput.println("</TBODY>");
           webPageOutput.println("</TABLE>");
           webPageOutput.println("</DIV>");
           }
        }
      else
        {
        webPageOutput.println(errorMessage);
        }
      webPageOutput.println(Routines.spaceLines(1));
      Routines.WriteHTMLTail(request,response,webPageOutput);
      }
   }