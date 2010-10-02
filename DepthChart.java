import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class DepthChart extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="DepthChart";

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
      Routines.WriteHTMLHead("Depth Chart",//title
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
      int season=Routines.safeParseInt((String)session.getAttribute("season"));
      int week=Routines.safeParseInt((String)session.getAttribute("viewWeek"));
      int teamNumber=Routines.safeParseInt((String)session.getAttribute("team"));
      int latestWeek=Routines.safeParseInt((String)session.getAttribute("week"));
      String teamName=(String)session.getAttribute("teamName");
      //Setup variables to calculate most frequently used formation.
      int[] chosenPlayers=new int[52];
      int   positionNumber=0;
      int   currentChosenPlayer=0;
      int[] positionNumbers={14,13,13,12,16,15,9,8,7,10,11,
                             17,18,18,17,20,19,19,20,21,27,26,21,
                             1,2,4,3};
      String[] positionCodes=new String[27];
      int startingPunter=0;
      int startingPR=0;
      int startingKR=0;
      boolean playerAlreadyUsed=false;
       try
         {
         Statement sql=database.createStatement();
         ResultSet queryResponse;
         for(int currentPosition=0;currentPosition<positionNumbers.length;currentPosition++)
            {
            queryResponse=sql.executeQuery("SELECT PositionCode " +
                                           "FROM positions " +
                                           "WHERE PositionNumber=" + positionNumbers[currentPosition]);
            queryResponse.first();
            positionCodes[currentPosition]=queryResponse.getString(1);
            }
         }
      catch(SQLException error)
         {
         Routines.writeToLog(servletName,"Database error retrieving position codes : " + error,false,context);
         }
      //Setup screen.
      Routines.titleHeader("head2",true,webPageOutput);
      webPageOutput.println("Depth Chart");
      Routines.titleTrailer(true,webPageOutput);
      webPageOutput.println("<DIV CLASS=\"SLTables2\">");
      // Declare all variables needed to contain player details.
      int[] startingPlayers        = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
      int[] startingPlayerNumbers  = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
      String[] startingPlayerNames = {"","","","","","","","","","","","","",
                                      "","","","","","","","","","","","","",""};
      int[] startingPlayersInjury  = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
      int[] startingPlayersDoctor  = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
      int[] backupPlayers          = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
      int[] backupPlayerNumbers    = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
      String[] backupPlayerNames   = {"","","","","","","","","","","","","",
                                      "","","","","","","","","","","","","",""};
      int[] backupPlayersInjury    = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
      int[] backupPlayersDoctor    = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
      // Retrieve Player information from DB
      try
         {
         int players[] = {0,0,0,0};
         Statement sql1=database.createStatement();
         Statement sql2=database.createStatement();
         ResultSet queryResponse1;
         ResultSet queryResponse2;
         int setNumber=0;
         queryResponse1=sql1.executeQuery("SELECT SetNumber " +
                                          "FROM sets " +
                                          "WHERE TeamNumber=" + teamNumber + " " +
                                          "AND SetName='Normal'");
         if(queryResponse1.first())
           {
           setNumber=queryResponse1.getInt(1);
           }
         else
           {
           queryResponse1=sql1.executeQuery("SELECT SetNumber " +
                                            "FROM defaultsets " +
                                            "WHERE SetName='Normal'");
           if(queryResponse1.first())
             {
             setNumber=queryResponse1.getInt(1);
             }
           else
             {
             Routines.writeToLog(servletName,"No normal set found (TeamNumber=" +
                                            teamNumber +
                                            ")",false,context);
             }
           }
         queryResponse1=sql1.executeQuery("SELECT PositionNumber " +
                                          "FROM positions");
         positionNumber=0;
         while(queryResponse1.next())
            {
            positionNumber=queryResponse1.getInt(1);
            queryResponse2 = sql2.executeQuery("SELECT depthcharts.PlayerNumber,Forname,Surname,Number,Injury,Doctors " +
                                               "FROM depthcharts,players " +
                                               "WHERE depthcharts.TeamNumber = " +
                                               teamNumber + " " +
                                               "AND depthcharts.TeamNumber = players.TeamNumber " +
                                               "AND depthcharts.PlayerNumber = players.PlayerNumber " +
                                               "AND depthcharts.PositionNumber = " +
                                               positionNumber + " " +
                                               "AND SetNumber = " + setNumber + " " +
                                               "ORDER BY Depth ASC");
            int playerNumber=0;
            String forname="";
            String surname="";
            int number=0;
            int injury=0;
            int doctors=0;
            while(queryResponse2.next())
              {
              playerNumber=queryResponse2.getInt(1);
              forname=queryResponse2.getString(2);
              surname=queryResponse2.getString(3);
              number=queryResponse2.getInt(4);
              injury=queryResponse2.getInt(5);
              doctors=queryResponse2.getInt(6);
              for(int displayDepth=0;displayDepth<2;displayDepth++)
                 {
                 for(int currentFormationPosition=0;currentFormationPosition<positionNumbers.length;currentFormationPosition++)
                    {
                    playerAlreadyUsed=false;
                    if(positionNumber==3||
                       positionNumber==8||
                       positionNumber==9)
                      {
                      if(positionNumber==3)
                        {
                        if(playerNumber==startingPunter)
                          {
                          playerAlreadyUsed=true;
                          }
                        else
                          {
                          if(displayDepth==0&&startingPunter==0)
                            {
                            startingPunter=playerNumber;
                            }
                          }
                        }
                      if(positionNumber==8)
                        {
                        if(playerNumber==startingPR)
                          {
                          playerAlreadyUsed=true;
                          }
                        else
                          {
                          if(displayDepth==0&&startingPR==0)
                            {
                            startingPR=playerNumber;
                            }
                          }
                        }
                      if(positionNumber==9)
                        {
                        if(playerNumber==startingKR)
                          {
                          playerAlreadyUsed=true;
                          }
                        else
                          {
                          if(displayDepth==0&&startingKR==0)
                            {
                            startingPR=playerNumber;
                            }
                          }
                        }
                      }
                    else
                      {
                        for(int currentPlayer=0;currentPlayer<chosenPlayers.length && !playerAlreadyUsed;currentPlayer++)
                           {
                           if(chosenPlayers[currentPlayer]==playerNumber)
                             {
                             playerAlreadyUsed=true;
                             }
                           }
                        }
                      if(!playerAlreadyUsed&&displayDepth==0&&startingPlayers[currentFormationPosition]==0)
                        {
                        startingPlayerNames[currentFormationPosition]=forname +
                                                                      " " +
                                                                      surname;
                        startingPlayerNumbers[currentFormationPosition]=number;
                        startingPlayers[currentFormationPosition]=playerNumber;
                        startingPlayersInjury[currentFormationPosition]=injury;
                        if(doctors==0)
                          {
                          startingPlayersInjury[currentFormationPosition]=injury;
                          }
                        else
                          {
                          if(injury>0)
                            {
                            startingPlayersInjury[currentFormationPosition]=injury+1;
                            }
                          else
                            {
                            startingPlayersInjury[currentFormationPosition]=injury;
                            }
                          }
                        startingPlayersDoctor[currentFormationPosition]=doctors;
                        chosenPlayers[currentChosenPlayer]=playerNumber;
                        currentChosenPlayer++;
                        }
                      if(!playerAlreadyUsed&&displayDepth==1&&backupPlayers[currentFormationPosition]==0)
                        {
                        backupPlayerNames[currentFormationPosition]=forname +
                                                                    " " +
                                                                    surname;
                        backupPlayerNumbers[currentFormationPosition]=number;
                        backupPlayers[currentFormationPosition]=playerNumber;
                        if(doctors==0)
                          {
                          backupPlayersInjury[currentFormationPosition]=injury;
                          }
                        else
                          {
                          if(injury>0)
                            {
                            backupPlayersInjury[currentFormationPosition]=injury+1;
                            }
                          else
                            {
                            backupPlayersInjury[currentFormationPosition]=injury;
                            }
                          }
                        backupPlayersDoctor[currentFormationPosition]=doctors;
                        chosenPlayers[currentChosenPlayer]=playerNumber;
                        currentChosenPlayer++;
                        }
                      }
               }
            }
          }
       }
      catch(SQLException error)
         {
         pool.returnConnection(database);
         Routines.writeToLog(servletName,"Database error retrieving players : " + error,false,context);
         }
      finally
         {
         pool.returnConnection(database);
         }

      for(int currentPosition=0;currentPosition<positionCodes.length;currentPosition++)
         {
         if(currentPosition==0)
           {
           tableHeader(teamName," Offense",true,webPageOutput);
           }
         if(currentPosition==11)
           {
           tableHeader(teamName," Defense",false,webPageOutput);
           }
         if(currentPosition==23)
           {
           tableHeader(teamName," Special Teams",false,webPageOutput);
           }
         webPageOutput.println("<TR HEIGHT=\"17\" CLASS=\"bg2\" ALIGN=\"right\">");
         webPageOutput.println("<TD ALIGN=\"left\">" + positionCodes[currentPosition] + "</TD>");
         if(startingPlayers[currentPosition]==0)
           {
           webPageOutput.println("<TD> </TD>");
           webPageOutput.println("<TD> </TD>");
           }
         else
           {
           webPageOutput.println("<TD ALIGN=\"left\">" + startingPlayerNumbers[currentPosition] + "</TD>");
           webPageOutput.println("<TD ALIGN=\"left\">");
           Routines.WriteHTMLLink(request,
                                         response,
                                         webPageOutput,
                                         "wfafl",
                                         "action=viewPlayer" +
                                         "&value=" +
                                         startingPlayers[currentPosition],
                                         startingPlayerNames[currentPosition],
                                         null,
                                         true);
           //Adjust number of weeks a starting player is still injured depending
           //on the week being previewed.
           int tempInjury=Routines.adjustInjuryLength(startingPlayersInjury[currentPosition],
                                                             latestWeek,
                                                             week);
           //Print starting player injury details.
           if(tempInjury>0)
             {
             webPageOutput.print("<FONT SIZE=\"1\"> &nbsp; ");
             if(tempInjury==1)
               {
               webPageOutput.print("Questionable");
               }
             else
               {
               webPageOutput.print("Out " + Routines.injuryText(startingPlayersInjury[currentPosition],
                                                                season,
                                                                latestWeek,
                                                                Routines.safeParseInt((String)session.getAttribute("preSeasonWeeks")),
                                                                Routines.safeParseInt((String)session.getAttribute("regularSeasonWeeks")),
                                                                Routines.safeParseInt((String)session.getAttribute("postSeasonWeeks")),
                                                                startingPlayersDoctor[currentPosition],
                                                                session));
               }
             webPageOutput.print("</FONT>");
             }
           webPageOutput.println("</TD>");
           }
         if(backupPlayers[currentPosition]==0)
           {
           webPageOutput.println("<TD> </TD>");
           webPageOutput.println("<TD> </TD>");
           }
         else
           {
           webPageOutput.println("<TD ALIGN=\"left\">" + backupPlayerNumbers[currentPosition] + "</TD>");
           webPageOutput.println("<TD ALIGN=\"left\">");
           Routines.WriteHTMLLink(request,
                                         response,
                                         webPageOutput,
                                         "wfafl",
                                         "action=viewPlayer" +
                                         "&value=" +
                                         backupPlayers[currentPosition],
                                         backupPlayerNames[currentPosition],
                                         null,
                                         true);
           //Adjust number of weeks a backup player is still injured depending
           //on the week being previewed.
           int tempInjury=Routines.adjustInjuryLength(backupPlayersInjury[currentPosition],
                                                             latestWeek,
                                                             week);
           //Print backup player injury details.
           if(tempInjury>0)
             {
             webPageOutput.print("<FONT SIZE=\"1\"> &nbsp; ");
             if(tempInjury==1)
               {
               webPageOutput.print("Questionable");
               }
             else
               {
               webPageOutput.print("Out " + Routines.injuryText(backupPlayersInjury[currentPosition],
                                                                season,
                                                                latestWeek,
                                                                Routines.safeParseInt((String)session.getAttribute("preSeasonWeeks")),
                                                                Routines.safeParseInt((String)session.getAttribute("regularSeasonWeeks")),
                                                                Routines.safeParseInt((String)session.getAttribute("postSeasonWeeks")),
                                                                backupPlayersDoctor[currentPosition],
                                                                session));
               }
             webPageOutput.print("</FONT>");
             }
           webPageOutput.println("</TD>");
           }
         webPageOutput.println("</TR>");
         }
      webPageOutput.println("</TBODY>");
      webPageOutput.println("</TABLE>");
      webPageOutput.println(Routines.spaceLines(1));
      Routines.WriteHTMLTail(request,response,webPageOutput);
      }

     static void tableHeader(String      teamName,
                             String      title,
                             boolean     first,
                             PrintWriter webPageOutput)
        {
        if(first)
          {
          webPageOutput.println("<TABLE WIDTH=\"100%\" CELLPADDING=\"2\" CELLAPSCING=\"1\" BORDER=\"0\">");
          webPageOutput.println("<TBODY>");
          }
        webPageOutput.println("<TR ALIGN=\"left\" CLASS=\"bg0\">");
        webPageOutput.println("<TD CLASS=\"home\" COLSPAN=\"5\">");
        webPageOutput.println("<FONT CLASS=\"home\">" + teamName + title + "</FONT>");
        webPageOutput.println("</TD>");
        webPageOutput.println("</TR>");
        webPageOutput.println("<TR ALIGN=\"center\" CLASS=\"bg4\">");
        webPageOutput.println("<TD>Position</TD>");
        webPageOutput.println("<TD COLSPAN=\"2\">First Team</TD>");
        webPageOutput.println("<TD COLSPAN=\"2\">Second Team</TD>");
        webPageOutput.println("</TR>");
        }
   }