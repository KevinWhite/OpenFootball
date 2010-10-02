import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class PlayByPlay extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="PlayByPlay";

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
      String action=request.getParameter("action");
      String loop=request.getParameter("loop");
      if(loop==null)
        {
        loop="";
        }
      if(loop.startsWith("true"))
        {
        session.setAttribute("redirect",request.getRequestURL() + "?" + request.getQueryString());
        }
      else
        {
        if("All Plays".equals(action))
          {
          session.setAttribute("redirect",request.getRequestURL() + "?" + request.getQueryString() + "&loop=true");
		  try
			{
			response.sendRedirect(Routines.getRedirect(request,response,context));
			}
		  catch(IOException error)
			{
			Routines.writeToLog(servletName,"Error redirecting : " + error,false,context);
			}
          return;
          }
        else
          {
          session.setAttribute("redirect",request.getRequestURL() + "?" + request.getQueryString() + "&loop=true#Latest");
		  try
			{
			response.sendRedirect(Routines.getRedirect(request,response,context));
			}
		  catch(IOException error)
			{
			Routines.writeToLog(servletName,"Error redirecting : " + error,false,context);
			}
          return;
          }
        }
      Connection database=null;
      try
        {
        database=pool.getConnection(servletName);
        }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Unable to connect to database : " + error,false,context);
        }
      Routines.WriteHTMLHead("Play By Play",//title
                             true,//showMenu
                             2,//menuHighLight
                             true,//seasonsMenu
		                     false,//weeksMenu
                             true,//scores
                             false,//standings
                             true,//gameCenter
                             false,//schedules
                             false,//previews
                             false,//teamCenter
		                     false,//draft
                             database,//database
                             request,//request
                             response,//response
                             webPageOutput,//webPageOutput
                             context);//context
      int fixtureNumber=Routines.safeParseInt((String)session.getAttribute("fixture"));
      String homeTeamName=(String)session.getAttribute("homeTeamName");
      String awayTeamName=(String)session.getAttribute("awayTeamName");
      int homeTeamScore=0;
      int awayTeamScore=0;
      int plays=0;
      int yards=0;
      int possession=0;
      int numberOfRecords=0;
      if("All Plays".equals(action))
        {
        numberOfRecords=999;
        }
      else
        {
        numberOfRecords=Routines.safeParseInt(request.getParameter("value2"));
        }
      boolean moreRecords=false;
      webPageOutput.println("<DIV CLASS=\"SLTables1\">");
      webPageOutput.println("<DIV CLASS=\"SLTables2\">");
      webPageOutput.println("<TABLE WIDTH=\"100%\" CELLPADDING=\"2\" CELLSPACING=\"1\" BORDER=\"0\">");
      webPageOutput.println("<TBODY>");
      try
         {
         Statement sql = database.createStatement();
         ResultSet queryResponse = sql.executeQuery("SELECT Clock,HomeTeamInPossession,Down,Distance," +
                                                    "DistanceToGoal,OffensiveFormationNumber,DefensiveFormationNumber," +
                                                    "PlayNumber,InitialBallCarrier,SecondaryBallCarrier,Defender," +
                                                    "OffensiveYards,DefensiveYards,Description," +
                                                    "Surname,Forname,PlayDescription,PlayNumber " +
                                                    "FROM   playbyplay,players,playdescriptions " +
                                                    "WHERE  Fixture = " + fixtureNumber + " " +
                                                    "AND    PlayerNumber = InitialBallCarrier " +
                                                    "AND    Description = PlayNumber " +
                                                    "ORDER  BY PlayByPlayNumber ASC");
         boolean[] quarters = {false,false,false,false,false};
         int[] quartersTime = {0,900,1800,2700,3600};
         String[] quarterNames = {"First Quarter","Second Quarter","Third Quarter","Fourth Quarter","OverTime"};
         int nextQuarter=0;
         int currentRecord=0;
         boolean homeTeamCurrentlyInPossession = false;
         while(queryResponse.next()&&currentRecord<numberOfRecords)
            {
            int clock                    = queryResponse.getInt(1);
            int homeTeamInPossessionInt  = queryResponse.getInt(2);
            int down                     = queryResponse.getInt(3);
            int distance                 = queryResponse.getInt(4);
            int distanceToGoal           = queryResponse.getInt(5);
            int offensiveFormation       = queryResponse.getInt(6);
            int defensiveFormation       = queryResponse.getInt(7);
            int play                     = queryResponse.getInt(8);
            int initialBallCarrier       = queryResponse.getInt(9);
            int secondaryBallCarrier     = queryResponse.getInt(10);
            int defender                 = queryResponse.getInt(11);
            int offensiveYards           = queryResponse.getInt(12);
            int defensiveYards           = queryResponse.getInt(13);
            int description              = queryResponse.getInt(14);
            String surName               = queryResponse.getString(15);
            String forName               = queryResponse.getString(16);
            String playDescription       = queryResponse.getString(17);
            int playDescriptionNo        = queryResponse.getInt(18);
            boolean homeTeamInPossession = false;
            String secondaryForname      = "";
            String secondarySurname      = "";
            String defenderForname       = "";
            String defenderSurname       = "";
            if (homeTeamInPossessionInt==1)
               {
               homeTeamInPossession=true;
               }
            if(secondaryBallCarrier!=0)
               {
               String playerNames[]={"",""};
               playerNames = getPlayerName(secondaryBallCarrier,database);
               secondaryForname = playerNames[0];
               secondarySurname = playerNames[1];
               }
            if(defender!=0)
               {
               String playerNames[]={"",""};
               playerNames = getPlayerName(defender,database);
               defenderForname = playerNames[0];
               defenderSurname = playerNames[1];
               }
            if(quarters[nextQuarter]==false && clock>=quartersTime[nextQuarter])
               {
               if(!quarters[0] || nextQuarter==2)
                  {
                  homeTeamCurrentlyInPossession=homeTeamInPossession;
                  }
               webPageOutput.println("<TR HEIGHT=\"17\" CLASS=\"bg2\" ALIGN=\"right\">");
               webPageOutput.println("<TD COLSPAN=\"2\" ALIGN=\"left\">");
               webPageOutput.println("<FONT SIZE=\"2\">");
               if(!quarters[0])
                 {
                 webPageOutput.println("<B>" +
                                       quarterNames[nextQuarter] +
                                       "</B>");
                 }
               else
                 {
                 webPageOutput.println("<B>" +
                                       quarterNames[nextQuarter] +
                                       " - " +
                                       awayTeamName +
                                       " " +
                                       awayTeamScore +
                                       " &nbsp; " +
                                       homeTeamName +
                                       " " +
                                       homeTeamScore +
                                       "</B>");
                 }
               webPageOutput.println("</FONT>");
               webPageOutput.println("</TD>");
               webPageOutput.println("</TR>");
               if (nextQuarter==0 || nextQuarter==2 || nextQuarter==4)
                  {
                  plays=0;
                  yards=0;
                  possession=clock;
                  changeOfPossession(homeTeamName,
                                     awayTeamName,
                                     homeTeamInPossession,
                                     clock,
                                     quartersTime[nextQuarter],
                                     webPageOutput);
                  }
               if(homeTeamInPossession==homeTeamCurrentlyInPossession &&
                  nextQuarter!=0 && nextQuarter!=2)
                  {
                  webPageOutput.println("<TR HEIGHT=\"17\" CLASS=\"bg2\" ALIGN=\"right\">");
                  webPageOutput.print("<TD COLSPAN=\"2\" CLASS=\"bg3\" ALIGN=\"left\"><B>");
                  if(homeTeamInPossession)
                     {
                     webPageOutput.print(homeTeamName);
                     }
                  else
                     {
                     webPageOutput.print(awayTeamName);
                     }
                  webPageOutput.println(" continued...</B></TD>");
                  webPageOutput.println("</TR>");
                  }
               quarters[nextQuarter]=true;
               nextQuarter++;
               }
            if(homeTeamInPossession!=homeTeamCurrentlyInPossession)
               {
               plays=0;
               yards=0;
               possession=clock;
               changeOfPossession(homeTeamName,
                                  awayTeamName,
                                  homeTeamInPossession,
                                  clock,
                                  quartersTime[nextQuarter - 1],
                                  webPageOutput);
               homeTeamCurrentlyInPossession=homeTeamInPossession;
               }
            int gameTime[] = giveGameTime(clock,quartersTime[nextQuarter - 1]);
            if(playDescriptionNo==56)
              {
              if(homeTeamInPossession)
                 {
                 homeTeamScore+=3;
                 }
              else
                 {
                 awayTeamScore+=3;
                 }
              }

            if(playDescriptionNo==81)
              {
              if(homeTeamInPossession)
                 {
                 awayTeamScore+=2;
                 }
              else
                 {
                 homeTeamScore+=2;
                 }
              }

            if(playDescriptionNo>=61 && playDescriptionNo<=70)
              {
              if(homeTeamInPossession)
                 {
                 homeTeamScore+=6;
                 }
              else
                 {
                 awayTeamScore+=6;
                 }
              }

            if(playDescriptionNo==84)
              {
              if(homeTeamInPossession)
                 {
                 homeTeamScore+=1;
                 }
              else
                 {
                 awayTeamScore+=1;
                 }
              }

            if(playDescriptionNo==86)
              {
              if(homeTeamInPossession)
                 {
                 homeTeamScore+=2;
                 }
              else
                 {
                 awayTeamScore+=2;
                 }
              }
            plays++;
            if(playDescriptionNo!=56)
              {
              yards+=offensiveYards;
              }
            printPlay(down,
                      distance,
                      distanceToGoal,
                      homeTeamInPossession,
                      homeTeamName,
                      awayTeamName,
                      gameTime,
                      forName,
                      surName,
                      offensiveYards,
                      playDescription,
                      secondaryBallCarrier,
                      secondaryForname,
                      secondarySurname,
                      defender,
                      defenderForname,
                      defenderSurname,
                      defensiveYards,
                      playDescriptionNo,
                      homeTeamScore,
                      awayTeamScore,
                      plays,
                      yards,
                      possession,
                      clock,
                      webPageOutput);
            if(playDescriptionNo==56 || playDescriptionNo==81  ||
              (playDescriptionNo>=84 && playDescriptionNo<=87))
              {
              changeOfPossession(homeTeamName,awayTeamName,homeTeamInPossession,clock,quartersTime[nextQuarter -1],webPageOutput);
              }
            currentRecord++;
            }
          moreRecords=queryResponse.next();
          if(numberOfRecords>0)
            {
            String currentScoreText="";
            if(moreRecords)
              {
              currentScoreText="Current Score";
              }
            else
              {
              currentScoreText="Final Score";
              }
            webPageOutput.println("<TR HEIGHT=\"17\" CLASS=\"bg2\" ALIGN=\"right\">");
            webPageOutput.println("<TD COLSPAN=\"2\" ALIGN=\"left\">");
            webPageOutput.println("<FONT SIZE=\"2\">");
            webPageOutput.println("<B>" +
                                  currentScoreText +
                                  " - " +
                                  awayTeamName +
                                  " " +
                                  awayTeamScore +
                                  " &nbsp; " +
                                  homeTeamName +
                                  " " +
                                  homeTeamScore +
                                  "</B>");
             webPageOutput.println("</FONT>");
             webPageOutput.println("</TD>");
             webPageOutput.println("</TR>");
             }
         }
      catch(SQLException error)
         {
		 Routines.writeToLog(servletName,"Database error retrieving PlayByPlay : " + error,false,context);		
         }
      finally
         {
         pool.returnConnection(database);
         }
      webPageOutput.println("</TBODY>");
      webPageOutput.println("</TABLE>");
      webPageOutput.println("</DIV>");
      webPageOutput.println("</DIV>");
      webPageOutput.println("<A NAME=\"Latest\"></A>");
      if(moreRecords)
         {
         webPageOutput.println("<BR>");
         webPageOutput.println("<CENTER>");
         webPageOutput.println("<FORM ACTION=\"http://" +
                                request.getServerName() +
                                ":" +
                                request.getServerPort() +
                                request.getContextPath() +
                                "/servlet/PlayByPlay\" METHOD=\"GET\">");
         if(numberOfRecords==0)
            {
            webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"First Play\" NAME=\"action\">");
            }
         else
            {
            webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Next Play\" NAME=\"action\">");
            }
         webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"All Plays\" NAME=\"action\">");
         webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
         webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"league\" VALUE=\"" + Routines.safeParseInt((String)session.getAttribute("league")) + "\">");
         webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"season\" VALUE=\"" + Routines.safeParseInt((String)session.getAttribute("season")) + "\">");
         webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"week\" VALUE=\"" + Routines.safeParseInt((String)session.getAttribute("week")) + "\">");
         webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"fixture\" VALUE=\"" + fixtureNumber + "\">");
         webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"value2\" VALUE=\"" + (numberOfRecords + 1) + "\">");
         webPageOutput.println("</FORM>");
         webPageOutput.println("</CENTER>");
         }
      int spaceLines=11 - (numberOfRecords * 2);
      if (spaceLines<1)
         {
         spaceLines=1;
         }
      webPageOutput.println(Routines.spaceLines(spaceLines));
      Routines.WriteHTMLTail(request,response,webPageOutput);
      }

   private void changeOfPossession(String      homeTeam,
                                  String      awayTeam,
                                  boolean     homeTeamInPossession,
                                  int         clock,
                                  int         quartersTime,
                                  PrintWriter webPageOutput)
      {
      webPageOutput.println("<TR HEIGHT=\"17\" CLASS=\"bg2\" ALIGN=\"right\">");
      String tempHomeTeamInPossession = "";
      String tempTeam = "";
      if (homeTeamInPossession)
         {
         tempTeam = homeTeam;
         tempHomeTeamInPossession = "home";
         }
      else
         {
         tempTeam = awayTeam;
         tempHomeTeamInPossession = "away";
         }
      webPageOutput.println("<TD COLSPAN=\"2\" CLASS=\"" +
                            tempHomeTeamInPossession +
                            "\" ALIGN=\"left\">");
      webPageOutput.print("<B>" + tempTeam + " at ");
      int[] gameTime = giveGameTime(clock,quartersTime);
      webPageOutput.print(gameTime[0] + ":");
      if(gameTime[1]<10)
         {
         webPageOutput.println("0" + gameTime[1]);
         }
      else
         {
         webPageOutput.println(gameTime[1]);
         }
      webPageOutput.print("</B>");
      webPageOutput.println("</TD>");
      webPageOutput.println("</TR>");
      }

   private int[] giveGameTime(int clock,
                              int quartersTime)
      {
      int[] clockValues={0,0};
      int timeIntoQuarter = 900 - (clock - quartersTime);
      clockValues[0] = timeIntoQuarter / 60;
      clockValues[1] = timeIntoQuarter - ( clockValues[0] * 60 );
      return clockValues;
      }

   private String[] getPlayerName(int playerNumber,
                                  Connection database)
      {
      String[] playerNames={"",""};
      try
         {
         Statement sql=database.createStatement();
         ResultSet queryResponse = sql.executeQuery(
                                    "SELECT ForName,Surname " +
                                    "FROM   players " +
                                    "WHERE  PlayerNumber = " + playerNumber);
         queryResponse.first();
         playerNames[0] = queryResponse.getString(1);
         playerNames[1] = queryResponse.getString(2);
         }
      catch(SQLException error)
         {
		 Routines.writeToLog(servletName,"Database error retrieving player (player=" +
		                                 playerNumber + "): " + error,false,context);	
         }
      return playerNames;
      }

   private void printPlay(int         down,
                          int         distance,
                          int         distanceToGoal,
                          boolean     homeTeamInPossession,
                          String      homeTeam,
                          String      awayTeam,
                          int[]       gameTime,
                          String      forName,
                          String      surName,
                          int         offensiveYards,
                          String      playDescription,
                          int         secondaryBallCarrier,
                          String      secondaryForname,
                          String      secondarySurname,
                          int         defender,
                          String      defenderForname,
                          String      defenderSurname,
                          int         defensiveYards,
                          int         playDescriptionNo,
                          int         homeTeamScore,
                          int         awayTeamScore,
                          int         plays,
                          int         yards,
                          int         possession,
                          int         clock,
                          PrintWriter webPageOutput)
      {
            webPageOutput.println("<TR HEIGHT=\"17\" CLASS=\"bg2\" ALIGN=\"right\">");
            webPageOutput.println("<TD CLASS=\"bg3\" ALIGN=\"left\">");
            if(playDescriptionNo<84 || playDescriptionNo>87)
               {
               if(playDescriptionNo==56 || playDescriptionNo==81 ||
                 (playDescriptionNo>=61 && playDescriptionNo<=70))
                 {
                 webPageOutput.print("<B>");
                 }
               webPageOutput.print(down + "-");
               if(distance<10)
                  {
                  webPageOutput.print("0" + distance);
                  }
               else
                  {
                  webPageOutput.print(distance);
                  }
               webPageOutput.print("-");
               if (distanceToGoal<50)
                  {
                  if(homeTeamInPossession)
                     {
                     webPageOutput.println(Routines.getInitials(awayTeam));
                     }
                  else
                     {
                     webPageOutput.println(Routines.getInitials(homeTeam));
                     }
                  }
               if (distanceToGoal==50)
                  {
                  webPageOutput.println("Midfield");
                  }
               if (distanceToGoal>50)
                  {
                  distanceToGoal = 100 - distanceToGoal;
                  if(homeTeamInPossession)
                     {
                     webPageOutput.println(Routines.getInitials(homeTeam));
                     }
                  else
                     {
                     webPageOutput.println(Routines.getInitials(awayTeam));
                     }
                  }
               webPageOutput.println(distanceToGoal);
               if(playDescriptionNo==56 || playDescriptionNo==81 ||
                 (playDescriptionNo>=61 && playDescriptionNo<=70))
                 {
                 webPageOutput.print("</B>");
                 }
               }
            webPageOutput.println("</TD>");
            webPageOutput.println("<TD CLASS=\"bg3\" ALIGN=\"left\">");
            if(playDescriptionNo==56 || playDescriptionNo==81  ||
              (playDescriptionNo>=61 && playDescriptionNo<=70) ||
              (playDescriptionNo>=84 && playDescriptionNo<=87))
              {
              webPageOutput.print("<B>");
              }
            webPageOutput.print("(" + gameTime[0] + ":");
            if(gameTime[1]<10)
               {
               webPageOutput.println("0" + gameTime[1] + ")");
               }
            else
               {
               webPageOutput.println(gameTime[1] + ")");
               }
            webPageOutput.println(" " +
                                  forName.substring(0,1) +
                                  "." +
                                  surName);
            webPageOutput.println(offensiveYards + "yd " + playDescription);
            if(secondaryBallCarrier!=0)
               {
               webPageOutput.println(" " +
                                     secondaryForname.substring(0,1) +
                                     "." +
                                     secondarySurname);
               }
            if(defender!=0)
               {
               webPageOutput.print(" (" +
                                   defenderForname.substring(0,1) +
                                   "." +
                                   defenderSurname);
               if(defensiveYards!=0)
                  {
                  webPageOutput.println(" " +
                                        defensiveYards +
                                        " yds)");
                  }
               else
                  {
                  webPageOutput.println(")");
                  }
               }
            webPageOutput.print(".");
            if(playDescriptionNo==56 || playDescriptionNo==81  ||
              (playDescriptionNo>=61 && playDescriptionNo<=70) ||
              (playDescriptionNo>=84 && playDescriptionNo<=87))
              {
              webPageOutput.print("</B>");
              }
            webPageOutput.println("</TD>");
            webPageOutput.println("</TR>");

            if(playDescriptionNo>=84 && playDescriptionNo<=87)
               {
               plays--;
               }

            if(playDescriptionNo==56 || playDescriptionNo==81  ||
              (playDescriptionNo>=84 && playDescriptionNo<=87))
              {
              int driveLength = clock - possession;
              int minutes = driveLength / 60;
              int seconds = driveLength - ( minutes * 60 );
              webPageOutput.println("<TR CLASS=\"bg2\" ALIGN=\"right\" HEIGHT=\"17\">");
              webPageOutput.print("<TD ALIGN=\"left\" COLSPAN=\"2\"><B>");
              webPageOutput.print(Routines.getInitials(awayTeam).toUpperCase() + " ");
              webPageOutput.print(awayTeamScore + " &nbsp; ");
              webPageOutput.print(Routines.getInitials(homeTeam).toUpperCase() + " ");
              webPageOutput.print(homeTeamScore + ", &nbsp; Plays: ");
              webPageOutput.print(plays + " &nbsp; Yards: ");
              webPageOutput.print(yards + " &nbsp; Possession: ");
              webPageOutput.print(minutes + ":" + seconds + ".");
              webPageOutput.println("</B></TD></TR>");
              }
      }
   }