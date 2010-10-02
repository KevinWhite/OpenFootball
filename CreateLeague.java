import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*; 

public class CreateLeague extends HttpServlet
   {
   private ConnectionPool pool;
   private ServletContext context;
   private static String servletName="CreateLeague";

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

   public void doPost(HttpServletRequest request,
                      HttpServletResponse response)
      {
      doGet(request,response);
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
      int titleWidth=20;
      int inputWidth=80;
      String action=request.getParameter("action");
      if ("Cancel".equals(action))
         {
         try
           {
           response.sendRedirect((String)session.getAttribute("redirect"));
           }
         catch(IOException error)
           {
           Routines.writeToLog(servletName,"Error redirecting : " + error,false,context);
           }
         return;
         }
      if("Create League".equals(action))
         {
         int leagueTemplate=Routines.safeParseInt(request.getParameter("type"));
         int leagueLevel=Routines.safeParseInt(request.getParameter("level"));
         int codeBase=Routines.safeParseInt(request.getParameter("code"));
         int alpha=0;
         int beta=0;
         int trial=0;
         int live=0;
         if(codeBase==1)
           {
           alpha=1;
           }
         if(codeBase==2)
           {
           beta=1;
           }
         if(codeBase==3)
           {
           trial=1;
           }
         if(codeBase==4)
           {
           live=1;
           }
         String leagueName=request.getParameter("leagueName");
         synchronized(this)
           {
         try
           {
           Statement sql1=database.createStatement();
           Statement sql2=database.createStatement();
           Statement sql3=database.createStatement();
           Statement sql4=database.createStatement();
           Statement sql5=database.createStatement();
           Statement sql6=database.createStatement();
           Statement sql7=database.createStatement();
           ResultSet queryResponse1;
           ResultSet queryResponse2;
           ResultSet queryResponse3;
           ResultSet queryResponse4;
           ResultSet queryResponse5;
           ResultSet queryResponse6;
		   // Retrieve latest playerSeason;
		   int playerSeason=0;
		   queryResponse1 = sql1.executeQuery("SELECT   (Value+2000) " +
											  "FROM     gamesettings " +
											  "WHERE    SettingName='Player Season'");
		   if(queryResponse1.first())
			 {
			 playerSeason = queryResponse1.getInt(1);
			 }
		   // Retrieve latest leagueNumber;
           int leagueNumber=1;
           queryResponse1 = sql1.executeQuery("SELECT   LeagueNumber " +
                                              "FROM     leagues " +
                                              "ORDER BY LeagueNumber DESC");
           if(queryResponse1.first())
             {
             leagueNumber = queryResponse1.getInt(1) + 1;
             }
           // Retrieve latest conferenceNumber;
           int conferenceNumber=0;
           queryResponse1 = sql1.executeQuery("SELECT   ConferenceNumber " +
                                              "FROM     conferences " +
                                              "ORDER BY ConferenceNumber DESC");
           if(queryResponse1.first())
             {
             conferenceNumber = queryResponse1.getInt(1);
             }
           // Retrieve latest divisionNumber;
           int divisionNumber=0;
           queryResponse1 = sql1.executeQuery("SELECT   DivisionNumber " +
                                              "FROM     divisions " +
                                              "ORDER BY DivisionNumber DESC");
           if(queryResponse1.first())
             {
             divisionNumber = queryResponse1.getInt(1);
             }
           // Retrieve latest teamNumber;
           int teamNumber=0;
           queryResponse1 = sql1.executeQuery("SELECT   teams.TeamNumber " +
                                              "FROM     teams,leagueteams,divisions,conferences " +
                                              "WHERE    teams.TeamNumber = leagueteams.TeamNumber " +
                                              "AND      leagueteams.DivisionNumber = divisions.DivisionNumber " +
                                              "AND      divisions.conferencenumber = conferences.ConferenceNumber " +
                                              "AND      conferences.leagueNumber = " +
                                              (leagueNumber - 1) + " " +
                                              "ORDER BY TeamNumber DESC");
           if(queryResponse1.first())
             {
             teamNumber = queryResponse1.getInt(1);
             }
           // Retrieve latest leagueTeamNumber;
           int leagueTeamNumber=0;
           queryResponse1=sql1.executeQuery("SELECT LeagueTeamNumber " +
                                            "FROM leagueteams " +
                                            "ORDER BY LeagueTeamNumber DESC");
           if(queryResponse1.first())
             {
             leagueTeamNumber = queryResponse1.getInt(1);
             }
           // Retrieve latest StandingNumber;
           int standingNumber=0;
           queryResponse1=sql1.executeQuery("SELECT   StandingNumber " +
                                            "FROM     standings " +
                                            "ORDER BY StandingNumber DESC");
           if(queryResponse1.first())
             {
             standingNumber=queryResponse1.getInt(1);
             }
           // Retrieve latest SituationNumber.
           int situationNumber=0;
           queryResponse1=sql1.executeQuery("SELECT   SituationNumber " +
                                            "FROM     situations " +
                                            "ORDER BY SituationNumber DESC");
           if(queryResponse1.first())
             {
             situationNumber=queryResponse1.getInt(1);
             }
           // Retrieve latest PlayBookNumber.
           int playBookNumber=0;
           queryResponse1=sql1.executeQuery("SELECT   PlayBookNumber " +
                                            "FROM     playbook " +
                                            "ORDER BY PlayBookNumber DESC");
           if(queryResponse1.first())
             {
             playBookNumber=queryResponse1.getInt(1);
             }
           // Retrieve latest SetNumber.
           int setNumber=0;
           queryResponse1=sql1.executeQuery("SELECT   SetNumber " +
                                            "FROM     sets " +
                                            "ORDER BY SetNumber DESC");
           if(queryResponse1.first())
             {
             setNumber=queryResponse1.getInt(1);
             }
           // Retrieve number of Staff Positions.
           int numOfStaffPositions=0;
           queryResponse1=sql1.executeQuery("SELECT PositionNumber " +
                                            "FROM positions " +
                                            "WHERE Type=3");
           while(queryResponse1.next())
                {
                numOfStaffPositions++;
                }
          int[] staffPositionNumber=new int[numOfStaffPositions];
          queryResponse1=sql1.executeQuery("SELECT PositionNumber " +
                                           "FROM positions " +
                                           "WHERE Type=3");
          int currentPosition=0;
          while(queryResponse1.next())
               {
               staffPositionNumber[currentPosition]=queryResponse1.getInt(1);
               currentPosition++;
               }
          // Retrieve LeagueTemplate details
          queryResponse1 = sql1.executeQuery("SELECT PreSeasonWeeks,RegularSeasonWeeks,PostSeasonWeeks,RosterMin,RosterMax,SalaryCap " +
                                             "FROM templateleagues " +
                                             "WHERE LeagueTemplate = " +
                                             leagueTemplate);
          queryResponse1.first();
          int preSeasonWeeks     = queryResponse1.getInt(1);
          int regularSeasonWeeks = queryResponse1.getInt(2);
          int postSeasonWeeks    = queryResponse1.getInt(3);
          int rosterMin          = queryResponse1.getInt(4);
          int rosterMax          = queryResponse1.getInt(5);
          int salaryCap          = queryResponse1.getInt(6);
          // Create new League based on LeagueTemplate details.
          sql1.executeUpdate("INSERT INTO leagues (LeagueNumber,LeagueType,Season,Week,Name,PreSeasonWeeks,RegularSeasonWeeks,PostSeasonWeeks,LeagueLevel,ScheduleInterConference,ScheduleIntraConference,Alpha,Beta,Trial,Live,WorldNumber,PlayerSeason,SeasonStart,DateTimeStamp,RosterMin,RosterMax,SalaryCap)" +
                             " VALUES ("  +
                             leagueNumber + "," +
                             leagueTemplate + "," +
                             "0,0,'" +
                             leagueName +
                             "'," +
                             preSeasonWeeks + "," +
                             regularSeasonWeeks + "," +
                             postSeasonWeeks + "," +
                             leagueLevel + ",1,1," +
                             alpha + "," +
                             beta + "," +
                             trial + "," +
                             live + "," +
                             leagueNumber + "," +
                             playerSeason + ",'" +
                             Routines.getDateTime(true) + "','" +
                             Routines.getDateTime(true) + "'," +
                             rosterMin + "," +
                             rosterMax + "," +
                             salaryCap + ")");
          // Create players
          int updates=sql1.executeUpdate("INSERT INTO players (" +
									     "WorldNumber,MasterPlayerNumber,DateTimeStamp) " +
									     "SELECT LeagueNumber,MasterPlayerNumber,masterplayers.DateTimeStamp FROM leagues,masterplayers " +
									     "WHERE LeagueNumber=" + leagueNumber + " " +
									     "AND PlayerSeason=masterplayers.Season");
		  if(updates==0)
		    {
		    Routines.writeToLog(servletName,"players not created",false,context);
		    }
		  // Retrieve ConferenceTemplates for Conferences Details for specified LeagueTemplate.
          queryResponse1=sql1.executeQuery("SELECT ConferenceTemplate,ConferenceName " +
                                           "FROM templateconferences " +
                                           "WHERE LeagueTemplate = " + leagueTemplate +
                                           " ORDER BY ConferenceTemplate ASC");
          while(queryResponse1.next())
             {
             String conferenceName = new String(queryResponse1.getString(2));
             int conferenceTemplate = queryResponse1.getInt(1);
             // Create new Conference based on ConferenceTemplate details.
             conferenceNumber++;
             sql2.executeUpdate("INSERT INTO conferences (ConferenceNumber,LeagueNumber,ConferenceName,DateTimeStamp)" +
                                " VALUES (" +
                                conferenceNumber +
                                "," +
                                leagueNumber +
                                ",'" +
                                conferenceName + "','" +
                                Routines.getDateTime(false) + "')" );
             // Retrieve DivisionTemplates for Divisions details for specified ConferenceTemplate.
             queryResponse2 = sql2.executeQuery("SELECT DivisionTemplate,DivisionName " +
                                                "FROM templatedivisions " +
                                                "WHERE ConferenceTemplate = " + conferenceTemplate +
                                                " ORDER BY DivisionTemplate ASC");
             while(queryResponse2.next())
                {
                String divisionName = new String(queryResponse2.getString(2));
                int divisionTemplate = queryResponse2.getInt(1);
                // Create new Division based on DivisionTemplate details.
                divisionNumber++;
                sql3.executeUpdate("INSERT INTO divisions (DivisionNumber,ConferenceNumber,DivisionName,DateTimeStamp)" +
                                   " VALUES (" +
                                   divisionNumber +
                                   "," +
                                   conferenceNumber +
                                   ",'" +
                                   divisionName + "','" +
                                   Routines.getDateTime(false) + "')" );
                // Retrieve TeamTemplates for Team details for specified DivisionTemplate.
                queryResponse3 = sql3.executeQuery("SELECT TeamName,BaseNumber,Ranking,ShirtColour,NumberColour,Outdoor,Grass,Capacity,NickName,CapHit " +
                                                   "FROM templateteams " +
                                                   "WHERE DivisionTemplate = " + divisionTemplate + " " +
                                                   "AND LeagueTemplate=" + leagueTemplate + " " +
                                                   "ORDER BY Ranking ASC");
                int position = -1;
                while(queryResponse3.next())
                   {
                   String teamName      = queryResponse3.getString(1);
                   int factor           = queryResponse3.getInt(2);
                   int ranking          = queryResponse3.getInt(3);
                   String shirtColour   = queryResponse3.getString(4);
                   String numberColour  = queryResponse3.getString(5);
                   int outdoor          = queryResponse3.getInt(6);
                   int grass            = queryResponse3.getInt(7);
                   int capacity         = queryResponse3.getInt(8);
				   String nickName      = queryResponse3.getString(9);
				   int capHit           = queryResponse3.getInt(10);
                   position++;
                   // Create new Teams based on TeamTemplates details.
                   sql4.executeUpdate("INSERT INTO teams (TeamNumber,Name,NickName,ShirtColour,NumberColour,Outdoor,Grass,Capacity,CapHit,DateTimeStamp)" +
                                      " VALUES (" +
                                      (teamNumber + factor) +
                                      ",'" +
                                      teamName + "','" +
					                  nickName + "','" +
                                      shirtColour + "','" +
                                      numberColour + "'," +
                                      outdoor + "," +
                                      grass + "," +
                                      capacity + "," +
                                      capHit + ",'" +
                                      Routines.getDateTime(false) + "')");
					// Create new GameBoard.
					sql4.executeUpdate("INSERT INTO gameboard (TeamNumber,DateTimeStamp)" +
									   " VALUES (" +
									   (teamNumber + factor) +
                                       ",'" + Routines.getDateTime(false) + "')");                                      
                   // Create LeagueTeams entry to link Team to league Structure.
                   leagueTeamNumber++;
                   sql4.executeUpdate("INSERT INTO leagueteams (LeagueTeamNumber,DivisionNumber,TeamNumber,Rank,DateTimeStamp)" +
                                      " VALUES (" +
                                      leagueTeamNumber +
                                      "," +
                                      divisionNumber +
                                      "," +
                                      (teamNumber + factor) +
                                      "," +
                                      ranking +
                                      ",'" +
                                      Routines.getDateTime(false) +
                                      "')");
                   // Create default Standings entry for Team.
                   standingNumber++;
                   sql4.executeUpdate("INSERT INTO standings (" +
                                      "StandingNumber,LeagueNumber,Season,Week,TeamNumber,CoachNumber,Position,DivisionNumber,DateTimeStamp)" +
                                      " VALUES (" +
                                      standingNumber +
                                      "," +
                                      leagueNumber +
                                      ",1,0," +
                                      (teamNumber + factor) +
                                      "," +
                                      factor +
                                      "," +
                                      position +
                                      "," +
                                      divisionNumber +
                                      ",'" +
                                      Routines.getDateTime(false) +
                                      "')");
                   // Create default Standings entry for CoachTeams.
                   if(leagueTemplate!=2)
                      {
					  sql4.executeUpdate("INSERT INTO coachteams (" +
									     "TeamNumber,CoachNumber,DateTimeStamp)" +
										 " VALUES (" +
										 (teamNumber + factor) +
										 "," +
										 factor +
										 ",'" +
										 Routines.getDateTime(false) +
										 "')");
                      }
                   // Create default Staff Draft Priorities.
                   int[] staffSequence=new int[numOfStaffPositions];
                   Random random = new Random(teamNumber + factor);
                   for(currentPosition=0;currentPosition<numOfStaffPositions;currentPosition++)
                      {
                      boolean notUsed=false;
                      while(!notUsed)
                        {
                        int randomNumber=((int)(random.nextDouble()*numOfStaffPositions)+1);
                        notUsed=true;
                        for(int currentPosition2=0;currentPosition2<numOfStaffPositions;currentPosition2++)
                           {
                           if(staffSequence[currentPosition2]==randomNumber)
                             {
                             notUsed=false;
                             }
                           }
                        if(notUsed)
                          {
                          staffSequence[currentPosition]=randomNumber;
                          }
                        }
                      }
                   for(currentPosition=0;currentPosition<numOfStaffPositions;currentPosition++)
                      {
                      sql4.executeUpdate("INSERT INTO draftpriorities (" +
                                         "LeagueNumber,TeamNumber,Sequence,PositionNumber,DateTimeStamp)" +
                                         " VALUES (" +
                                         leagueNumber +
                                         "," +
                                         (teamNumber + factor) +
                                         "," +
                                         staffSequence[currentPosition] +
                                         "," +
                                         staffPositionNumber[currentPosition] +
                                         ",'" +
                                         Routines.getDateTime(false) +
                                         "')");
                      }
					if(leagueLevel==3)
                      {
                      // Create default sets for team.
                      queryResponse4=sql4.executeQuery("SELECT Sequence,SetName " +
                                                       "FROM defaultsets " +
                                                       "ORDER BY SetNumber ASC");
                      while(queryResponse4.next())
                           {
                           setNumber++;
                           int sequence=queryResponse4.getInt(1);
                           String setName=queryResponse4.getString(2);
                           int updated=0;
                           updated=sql5.executeUpdate("INSERT INTO sets (" +
                                                      "SetNumber,TeamNumber," +
                                                      "SetName,Sequence," +
                                                      "DateTimeStamp) " +
                                                      "VALUES (" +
                                                      setNumber + "," +
                                                      (teamNumber+factor) + ",'" +
                                                      setName + "'," +
                                                      sequence + ",'" +
                                                      Routines.getDateTime(false) +
                                                      "')");
                             if(updated==0)
                               {
                               Routines.writeToLog(servletName,"No default sets created (TeamNumber=" + teamNumber + ")",false,context);
                               }
                           }
                      // Create default situations for team.
                      queryResponse4=sql4.executeQuery("SELECT SituationName,Type,Sequence," +
                                                       "Down1,Down2,Down3,Down4," +
                                                       "TimeFrom,TimeTo," +
                                                       "YdsDownFrom,YdsDownTo," +
                                                       "YdsScoreFrom,YdsScoreTo," +
                                                       "PointsFrom,PointsTo," +
                                                       "PlayOrderNumber,SituationCallNumber1," +
                                                       "SituationCallNumber2,SituationCallNumber3," +
                                                       "SituationCallNumber4,SituationCallNumber5," +
                                                       "SituationCallNumber6,SituationCallNumber7," +
                                                       "SituationCallNumber8,SituationCallNumber9," +
                                                       "SituationCallNumber10,SituationCallNumber11," +
                                                       "SituationCallNumber12,SituationCallNumber13," +
                                                       "SituationCallNumber14,SituationCallNumber15," +
                                                       "SituationCallNumber16,SituationCallNumber17," +
                                                       "SituationCallNumber18,SituationCallNumber19," +
                                                       "SituationCallNumber20,SituationNumber " +
                                                       "FROM defaultsituations " +
                                                       "ORDER BY SituationNumber ASC");
                      while(queryResponse4.next())
                           {
                           situationNumber++;
                           String situationName=queryResponse4.getString(1);
                           int type=queryResponse4.getInt(2);
                           int sequence=queryResponse4.getInt(3);
                           int down1=queryResponse4.getInt(4);
                           int down2=queryResponse4.getInt(5);
                           int down3=queryResponse4.getInt(6);
                           int down4=queryResponse4.getInt(7);
                           int timeFrom=queryResponse4.getInt(8);
                           int timeTo=queryResponse4.getInt(9);
                           int ydsDownFrom=queryResponse4.getInt(10);
                           int ydsDownTo=queryResponse4.getInt(11);
                           int ydsScoreFrom=queryResponse4.getInt(12);
                           int ydsScoreTo=queryResponse4.getInt(13);
                           int pointsFrom=queryResponse4.getInt(14);
                           int pointsTo=queryResponse4.getInt(15);
                           int playOrderNumber=queryResponse4.getInt(16);
                           int situationCallNumber1=queryResponse4.getInt(17);
                           int situationCallNumber2=queryResponse4.getInt(18);
                           int situationCallNumber3=queryResponse4.getInt(19);
                           int situationCallNumber4=queryResponse4.getInt(20);
                           int situationCallNumber5=queryResponse4.getInt(21);
                           int situationCallNumber6=queryResponse4.getInt(22);
                           int situationCallNumber7=queryResponse4.getInt(23);
                           int situationCallNumber8=queryResponse4.getInt(24);
                           int situationCallNumber9=queryResponse4.getInt(25);
                           int situationCallNumber10=queryResponse4.getInt(26);
                           int situationCallNumber11=queryResponse4.getInt(27);
                           int situationCallNumber12=queryResponse4.getInt(28);
                           int situationCallNumber13=queryResponse4.getInt(29);
                           int situationCallNumber14=queryResponse4.getInt(30);
                           int situationCallNumber15=queryResponse4.getInt(31);
                           int situationCallNumber16=queryResponse4.getInt(32);
                           int situationCallNumber17=queryResponse4.getInt(33);
                           int situationCallNumber18=queryResponse4.getInt(34);
                           int situationCallNumber19=queryResponse4.getInt(35);
                           int situationCallNumber20=queryResponse4.getInt(36);
                           int defaultSituationNumber=queryResponse4.getInt(37);
                           int updated=0;
                           updated=sql5.executeUpdate("INSERT INTO situations (" +
                                                      "SituationNumber,TeamNumber," +
                                                      "SituationName,Type,Sequence," +
                                                      "Down1,Down2,Down3,Down4," +
                                                      "TimeFrom,TimeTo," +
                                                      "YdsDownFrom,YdsDownTo," +
                                                      "YdsScoreFrom,YdsScoreTo," +
                                                      "PointsFrom,PointsTo," +
                                                      "PlayOrderNumber," +
                                                      "SituationCallNumber1,SituationCallNumber2," +
                                                      "SituationCallNumber3,SituationCallNumber4," +
                                                      "SituationCallNumber5,SituationCallNumber6," +
                                                      "SituationCallNumber7,SituationCallNumber8," +
                                                      "SituationCallNumber9,SituationCallNumber10," +
                                                      "SituationCallNumber11,SituationCallNumber12," +
                                                      "SituationCallNumber13,SituationCallNumber14," +
                                                      "SituationCallNumber15,SituationCallNumber16," +
                                                      "SituationCallNumber17,SituationCallNumber18," +
                                                      "SituationCallNumber19,SituationCallNumber20," +
                                                      "DateTimeStamp) " +
                                                      "VALUES (" +
                                                      situationNumber + "," +
                                                      (teamNumber+factor) + ",'" +
                                                      situationName + "'," +
                                                      type + "," +
                                                      sequence + "," +
                                                      down1 + "," +
                                                      down2 + "," +
                                                      down3 + "," +
                                                      down4 + "," +
                                                      timeFrom + "," +
                                                      timeTo + "," +
                                                      ydsDownFrom + "," +
                                                      ydsDownTo + "," +
                                                      ydsScoreFrom + "," +
                                                      ydsScoreTo + "," +
                                                      pointsFrom + "," +
                                                      pointsTo + "," +
                                                      playOrderNumber + "," +
                                                      situationCallNumber1 + "," +
                                                      situationCallNumber2 + "," +
                                                      situationCallNumber3 + "," +
                                                      situationCallNumber4 + "," +
                                                      situationCallNumber5 + "," +
                                                      situationCallNumber6 + "," +
                                                      situationCallNumber7 + "," +
                                                      situationCallNumber8 + "," +
                                                      situationCallNumber9 + "," +
                                                      situationCallNumber10 + "," +
                                                      situationCallNumber11 + "," +
                                                      situationCallNumber12 + "," +
                                                      situationCallNumber13 + "," +
                                                      situationCallNumber14 + "," +
                                                      situationCallNumber15 + "," +
                                                      situationCallNumber16 + "," +
                                                      situationCallNumber17 + "," +
                                                      situationCallNumber18 + "," +
                                                      situationCallNumber19 + "," +
                                                      situationCallNumber20 + ",'" +
                                                      Routines.getDateTime(false) +
                                                      "')");
                             if(updated==0)
                               {
                               Routines.writeToLog(servletName,"No default situations created (TeamNumber=" + teamNumber + ")",false,context);
                               }
                             queryResponse5=sql5.executeQuery("SELECT sets.SetNumber,defaultsets.SetNumber " +
                                                              "FROM sets,defaultsets " +
                                                              "WHERE TeamNumber = " + (teamNumber+factor) + " " +
                                                              "AND sets.Sequence = defaultsets.Sequence " +
                                                              "ORDER BY sets.Sequence ASC");
                             while(queryResponse5.next())
                                  {
                                  int newSetNumber=queryResponse5.getInt(1);
                                  int defaultSetNumber=queryResponse5.getInt(2);
                                  // Create default playbook for team.
                                  queryResponse6=sql6.executeQuery("SELECT Sequence," +
                                                                   "FormationNumber,PositionNumber,PlayNumber,PrimaryStrategyNumber," +
                                                                   "SecondaryStrategyNumber1,SecondaryStrategyNumber2," +
                                                                   "SecondaryStrategyNumber3,SecondaryStrategyNumber4," +
                                                                   "SecondaryStrategyNumber5,SecondaryStrategyNumber6," +
                                                                   "SecondaryStrategyNumber7,SecondaryStrategyNumber8," +
                                                                   "SecondaryStrategyNumber9,SecondaryStrategyNumber10 " +
                                                                   "FROM defaultplaybook " +
                                                                   "WHERE SituationNumber = " + defaultSituationNumber + " " +
                                                                   "AND SetNumber = " + defaultSetNumber + " " +
                                                                   "ORDER BY PlayBookNumber ASC");
                                  while(queryResponse6.next())
                                       {
                                       playBookNumber++;
                                       sequence=queryResponse6.getInt(1);
                                       int formationNumber=queryResponse6.getInt(2);
                                       int positionNumber=queryResponse6.getInt(3);
                                       int playNumber=queryResponse6.getInt(4);
                                       int primaryStrategyNumber=queryResponse6.getInt(5);
                                       int secondaryStrategyNumber1=queryResponse6.getInt(6);
                                       int secondaryStrategyNumber2=queryResponse6.getInt(7);
                                       int secondaryStrategyNumber3=queryResponse6.getInt(8);
                                       int secondaryStrategyNumber4=queryResponse6.getInt(9);
                                       int secondaryStrategyNumber5=queryResponse6.getInt(10);
                                       int secondaryStrategyNumber6=queryResponse6.getInt(11);
                                       int secondaryStrategyNumber7=queryResponse6.getInt(12);
                                       int secondaryStrategyNumber8=queryResponse6.getInt(13);
                                       int secondaryStrategyNumber9=queryResponse6.getInt(14);
                                       int secondaryStrategyNumber10=queryResponse6.getInt(15);
                                       updated=0;
                                       updated=sql7.executeUpdate("INSERT INTO playbook (" +
                                                                  "PlayBookNumber,TeamNumber," +
                                                                  "SituationNumber,Sequence," +
                                                                  "SetNumber,FormationNumber,PositionNumber,PlayNumber,PrimaryStrategyNumber," +
                                                                  "SecondaryStrategyNumber1,SecondaryStrategyNumber2," +
                                                                  "SecondaryStrategyNumber3,SecondaryStrategyNumber4," +
                                                                  "SecondaryStrategyNumber5,SecondaryStrategyNumber6," +
                                                                  "SecondaryStrategyNumber7,SecondaryStrategyNumber8," +
                                                                  "SecondaryStrategyNumber9,SecondaryStrategyNumber10," +
                                                                  "DateTimeStamp) " +
                                                                  "VALUES (" +
                                                                  playBookNumber + "," +
                                                                  (teamNumber+factor) + "," +
                                                                  situationNumber + "," +
                                                                  sequence + "," +
                                                                  newSetNumber + "," +
                                                                  formationNumber + "," +
                                                                  positionNumber + "," +
                                                                  playNumber + "," +
                                                                  primaryStrategyNumber + "," +
                                                                  secondaryStrategyNumber1 + "," +
                                                                  secondaryStrategyNumber2 + "," +
                                                                  secondaryStrategyNumber3 + "," +
                                                                  secondaryStrategyNumber4 + "," +
                                                                  secondaryStrategyNumber5 + "," +
                                                                  secondaryStrategyNumber6 + "," +
                                                                  secondaryStrategyNumber7 + "," +
                                                                  secondaryStrategyNumber8 + "," +
                                                                  secondaryStrategyNumber9 + "," +
                                                                  secondaryStrategyNumber10 + ",'" +
                                                                  Routines.getDateTime(false) + "')");
                                       }
                                  if(updated==0)
                                    {
                                    Routines.writeToLog(servletName,"No default playbook created (TeamNumber=" + teamNumber + ")",false,context);
                                    }
                                 }
                             }
                         }
                       else
                         {
                         // Create default playbook for team.
						 queryResponse5=sql5.executeQuery("SELECT SituationNumber,Sequence," +
                                                          "SetNumber,FormationNumber,PositionNumber,PlayNumber,PrimaryStrategyNumber," +
                                                          "SecondaryStrategyNumber1,SecondaryStrategyNumber2," +
                                                          "SecondaryStrategyNumber3,SecondaryStrategyNumber4," +
                                                          "SecondaryStrategyNumber5,SecondaryStrategyNumber6," +
                                                          "SecondaryStrategyNumber7,SecondaryStrategyNumber8," +
                                                          "SecondaryStrategyNumber9,SecondaryStrategyNumber10 " +
                                                          "FROM defaultplaybook " +
                                                          "ORDER BY PlayBookNumber ASC");
                         while(queryResponse5.next())
                              {
                              playBookNumber++;
                              situationNumber=queryResponse5.getInt(1);
                              int sequence=queryResponse5.getInt(2);
                              int tempSetNumber=queryResponse5.getInt(3);
                              int formationNumber=queryResponse5.getInt(4);
                              int positionNumber=queryResponse5.getInt(5);
                              int playNumber=queryResponse5.getInt(6);
                              int primaryStrategyNumber=queryResponse5.getInt(7);
                              int secondaryStrategyNumber1=queryResponse5.getInt(8);
                              int secondaryStrategyNumber2=queryResponse5.getInt(9);
                              int secondaryStrategyNumber3=queryResponse5.getInt(10);
                              int secondaryStrategyNumber4=queryResponse5.getInt(11);
                              int secondaryStrategyNumber5=queryResponse5.getInt(12);
                              int secondaryStrategyNumber6=queryResponse5.getInt(13);
                              int secondaryStrategyNumber7=queryResponse5.getInt(14);
                              int secondaryStrategyNumber8=queryResponse5.getInt(15);
                              int secondaryStrategyNumber9=queryResponse5.getInt(16);
                              int secondaryStrategyNumber10=queryResponse5.getInt(17);
                              int updated=0;
                              updated=sql6.executeUpdate("INSERT INTO playbook (" +
                                                         "PlayBookNumber,TeamNumber," +
                                                         "SituationNumber,Sequence," +
                                                         "SetNumber,FormationNumber,PositionNumber,PlayNumber,PrimaryStrategyNumber," +
                                                         "SecondaryStrategyNumber1,SecondaryStrategyNumber2," +
                                                         "SecondaryStrategyNumber3,SecondaryStrategyNumber4," +
                                                         "SecondaryStrategyNumber5,SecondaryStrategyNumber6," +
                                                         "SecondaryStrategyNumber7,SecondaryStrategyNumber8," +
                                                         "SecondaryStrategyNumber9,SecondaryStrategyNumber10," +
                                                         "DateTimeStamp) " +
                                                         "VALUES (" +
                                                         playBookNumber + "," +
                                                         (teamNumber+factor) + "," +
                                                         situationNumber + "," +
                                                         sequence + "," +
                                                         tempSetNumber + "," +
                                                         formationNumber + "," +
                                                         positionNumber + "," +
                                                         playNumber + "," +
                                                         primaryStrategyNumber + "," +
                                                         secondaryStrategyNumber1 + "," +
                                                         secondaryStrategyNumber2 + "," +
                                                         secondaryStrategyNumber3 + "," +
                                                         secondaryStrategyNumber4 + "," +
                                                         secondaryStrategyNumber5 + "," +
                                                         secondaryStrategyNumber6 + "," +
                                                         secondaryStrategyNumber7 + "," +
                                                         secondaryStrategyNumber8 + "," +
                                                         secondaryStrategyNumber9 + "," +
                                                         secondaryStrategyNumber10 + ",'" +
                                                         Routines.getDateTime(false) + "')");
                              if(updated==0)
                                {
                                Routines.writeToLog(servletName,"No default playbook created (TeamNumber=" + teamNumber + ")",false,context);
                                }
                            }
                         }
                    }
                }
             }
          //Create Schedule
          Routines.scheduler(leagueNumber,database,context);
          if(leagueTemplate==2)
             {
             ResultSet queryResult=sql1.executeQuery("SELECT NickName,teams.TeamNumber " +
                                                     "FROM teams,conferences,divisions,leagueteams " +
                                                     "WHERE conferences.LeagueNumber=" + leagueNumber + " " +
                                                     "AND conferences.ConferenceNumber=divisions.ConferenceNumber " +
                                                     "AND divisions.DivisionNumber=leagueteams.DivisionNumber " +
                                                     "AND leagueteams.TeamNumber=teams.TeamNumber");	
             while(queryResult.next())
               {
               teamNumber=queryResult.getInt(2);	
               if(playerSeason==2007)
                 {
				 int[] contractValues={250,500,750,1000,1250,1500,1750,2000,2250,2500,2750,3000,3250,3500,3750,4000,4250,4500,4750,5000,300,350,1200,400,600,800,550,375,625,650,1300,1100,1550,950,1650,900,450,875};
				 String[] contractText=new String[contractValues.length];	
				 boolean shirt[]= new boolean[100];	
				 int coachNumber=0;
                 if(queryResult.getString(1).equals("Penguins"))
                   {
                   //Allocate UNFL Players to Chicago
                   coachNumber=16;
                   sql2.executeUpdate("UPDATE players SET TeamNumber="+teamNumber + " "+
                                      "WHERE WorldNumber="+leagueNumber + " " +
                                      "AND MasterPlayerNumber IN (6657,6980,7454,6480,7446,7956," +
                                      "7824,6521,7497,6908,7993,7068,8629," +
                                      "8304,6779,6676,7844,7066,0000,8550,6791," +
                                      "8491,8151,6435,7217,8177,7305," +
                                      "6607,8456,6643,7292,6460,7286," +
                                      "7204,7766,8438,6767,6920," +
                                      "7471,7524,8174," +
						              "0000,0000,0000,0000,0000" +                                      ")");
                    //Set players contracts
                    contractText[0]="7956,8174,6791";//0.25m
					contractText[1]="6980,6908,7993,6676,7524";//0.50m
					contractText[2]="8304,6779,7471";//0.75m
					contractText[3]="7066,6920";//1.00m
					contractText[4]="7286,8438";//1.25m
					contractText[5]="7844,7766";//1.50m
					contractText[6]="7497,7292,6460,7204";//1.75m
					contractText[7]="6480,7446,6521,8550,6435,6643,6767";//2.00m
					contractText[8]="8456";//2.25m
					contractText[9]="7454,6607";//2.50m
					contractText[10]="7824,8629,8491,8151";//2.75m
					contractText[11]="7068,7217,8177,7305,6657";//3.00m
					contractText[12]="0";//3.25m
					contractText[13]="0";//3.50m
					contractText[14]="0";//3.75m
					contractText[15]="0";//4.00m
					contractText[16]="0";//4.25m
					contractText[17]="0";//4.50m
					contractText[18]="0";//4.75m
					contractText[19]="0";//5.00m
					contractText[20]="0";//0.30m
					contractText[21]="0";//0.35m
					contractText[22]="0";//1.20m
					contractText[23]="0";//0.40m
					contractText[24]="0";//0.60m
					contractText[25]="0";//0.80m
					contractText[26]="0";//0.55m
					contractText[27]="0";//0.375m
					contractText[28]="0";//0.625m
					contractText[29]="0";//0.65m
					contractText[30]="0";//1.3m
					contractText[31]="0";//1.1m
					contractText[32]="0";//1.55m
					contractText[33]="0";//0.95m
					contractText[34]="0";//1.65m
					contractText[35]="0";//0.9m
					contractText[36]="0";//0.45m
					contractText[37]="0";//0.875m
                    }
				  if(queryResult.getString(1).equals("Magic"))
				    {
					//Allocate UNFL Players to Orlando
					coachNumber=7;
					sql2.executeUpdate("UPDATE players SET TeamNumber="+teamNumber + " "+
									   "WHERE WorldNumber="+leagueNumber + " " +
									   "AND MasterPlayerNumber IN (6597,8093,7689,8342,7536,7102," +
									   "6832,6914,6633,6925,7255,6860,7529," +
									   "6439,7737,8519,7423,7165,7475,6883,8534," +
									   "8069,6903,7626,7006,8223,7790," +
									   "8179,8427,7343,7452,6880,7591," +
									   "8172,8575,7218,6993,6700," +
									   "7690,8561," +
						               "0000,0000,0000,0000,0000" +
									   ")");
					 //Set players contracts
					 contractText[0]="7165,6700";//0.25m
					 contractText[1]="8093,7536,6914,7626";//0.50m
					 contractText[2]="7423,8172";//0.75m
					 contractText[3]="6860,6439,6993,7690,8561";//1.00m
					 contractText[4]="8534,7343,7591";//1.25m
					 contractText[5]="6883,8179,8427,6880";//1.50m
					 contractText[6]="6832,7255,8519,8069,6903,7452,7218";//1.75m
					 contractText[7]="8342,7529,7790,8575";//2.00m
					 contractText[8]="6925,8223";//2.25m
					 contractText[9]="7737";//2.50m
					 contractText[10]="7689,6633";//2.75m
					 contractText[11]="6597,7102,7475,7006";//3.00m
					 contractText[12]="0";//3.25m
					 contractText[13]="0";//3.50m
					 contractText[14]="0";//3.75m
					 contractText[15]="0";//4.00m
					 contractText[16]="0";//4.25m
					 contractText[17]="0";//4.50m
					 contractText[18]="0";//4.75m
					 contractText[19]="0";//5.00m
					 contractText[20]="0";//0.30m
					 contractText[21]="0";//0.35m
					 contractText[22]="0";//1.20m
					 contractText[23]="0";//0.40m
					 contractText[24]="0";//0.60m
					 contractText[25]="0";//0.80m
					 contractText[26]="0";//0.55m
					 contractText[27]="0";//0.375m
					 contractText[28]="0";//0.625m
					 contractText[29]="0";//0.65m
					 contractText[30]="0";//1.3m
					 contractText[31]="0";//1.1m
					 contractText[32]="0";//1.55m
					 contractText[33]="0";//0.95m
					 contractText[34]="0";//1.65m
					 contractText[35]="0";//0.9m
					 contractText[36]="0";//0.45m
					 contractText[37]="0";//0.875m
                     }       
				  if(queryResult.getString(1).equals("Versaces"))
					{
					//Allocate UNFL Players to Munich
					coachNumber=17;
					sql2.executeUpdate("UPDATE players SET TeamNumber="+teamNumber + " "+
									   "WHERE WorldNumber="+leagueNumber + " " +
									   "AND MasterPlayerNumber IN (8068,7919,6966,7907,6660,7186," +
									   "8248,6748,7489,7043,7779,7889,7157," +
									   "7674,8588,7717,7370,7393,6488,6788,8665," +
									   "7848,6557,8522,8618,6740,7342," +
									   "8036,8578,7023,6844,7493,7598," +
									   "8548,8009,6580,8220,7566," +
									   "7767,8416," +
						               "0000,0000,0000,0000,0000" +
									   ")");
					//Set players contracts
					contractText[0]="0";//0.25m
					contractText[1]="8036,7023,8009,8220,7566,7767,8416";//0.50m
					contractText[2]="7717,8618,6580";//0.75m
					contractText[3]="7489,7342,7598";//1.00m
					contractText[4]="7186,6844,7493";//1.25m
					contractText[5]="7043,7779,7393,6788,7848";//1.50m
					contractText[6]="7370,8665,8548";//1.75m
					contractText[7]="8068,7919,7889,6740";//2.00m
					contractText[8]="8588,6557,8578";//2.25m
					contractText[9]="6966,8522";//2.50m
					contractText[10]="7674";//2.75m
					contractText[11]="7907,6660,8248,6748,7157,6488";//3.00m
					contractText[12]="0";//3.25m
					contractText[13]="0";//3.50m
					contractText[14]="0";//3.75m
					contractText[15]="0";//4.00m
					contractText[16]="0";//4.25m
					contractText[17]="0";//4.50m
					contractText[18]="0";//4.75m
					contractText[19]="0";//5.00m
					contractText[20]="0";//0.30m
					contractText[21]="0";//0.35m
					contractText[22]="0";//1.20m
					contractText[23]="0";//0.40m
					contractText[24]="0";//0.60m
					contractText[25]="0";//0.80m
					contractText[26]="0";//0.55m
					contractText[27]="0";//0.375m
					contractText[28]="0";//0.625m
					contractText[29]="0";//0.65m
					contractText[30]="0";//1.3m
					contractText[31]="0";//1.1m
					contractText[32]="0";//1.55m
					contractText[33]="0";//0.95m
					contractText[34]="0";//1.65m
					contractText[35]="0";//0.9m
					contractText[36]="0";//0.45m
					contractText[37]="0";//0.875m
					}
				  if(queryResult.getString(1).equals("Coyotes"))
					{
					//Allocate UNFL Players to Kansas
					coachNumber=4;
					sql2.executeUpdate("UPDATE players SET TeamNumber="+teamNumber + " "+
									   "WHERE WorldNumber="+leagueNumber + " " +
									   "AND MasterPlayerNumber IN (7324,8487,7422,7017,7843,8171," +
									   "6792,7817,8279,7745,8514,7883,8286," +
									   "8188,6420,7274,8022,7163,7034,8320,7321," +
									   "7150,8084,7957,8496,7727,8613," +
									   "7013,6469,7852,6560,6814,8345," +
									   "8042,7624,6902,8024,7801," +
									   "6407,7272," +
						               "0000,0000,0000,0000,0000" +
									   ")");
					//Set players contracts
					contractText[0]="7745,8514,7727,6902,8024";//0.25m
					contractText[1]="8487,8171,6814,6407";//0.50m
					contractText[2]="0";//0.75m
					contractText[3]="7843,8286,7801";//1.00m
					contractText[4]="7017,7274,8022,6560";//1.25m
					contractText[5]="7163,7321";//1.50m
					contractText[6]="7150,7013,7624";//1.75m
					contractText[7]="7324,7034,8084,8496,7852,8042,7272";//2.00m
					contractText[8]="8613";//2.25m
					contractText[9]="8188,6420,8345";//2.50m
					contractText[10]="6792,8279";//2.75m
					contractText[11]="7817,7883,8320,7957";//3.00m
					contractText[12]="0";//3.25m
					contractText[13]="7422,6469";//3.50m
					contractText[14]="0";//3.75m
					contractText[15]="0";//4.00m
					contractText[16]="0";//4.25m
					contractText[17]="0";//4.50m
					contractText[18]="0";//4.75m
					contractText[19]="0";//5.00m
					contractText[20]="0";//0.30m
					contractText[21]="0";//0.35m
					contractText[22]="0";//1.20m
					contractText[23]="0";//0.40m
					contractText[24]="0";//0.60m
					contractText[25]="0";//0.80m
					contractText[26]="0";//0.55m
					contractText[27]="0";//0.375m
					contractText[28]="0";//0.625m
					contractText[29]="0";//0.65m
					contractText[30]="0";//1.3m
					contractText[31]="0";//1.1m
					contractText[32]="0";//1.55m
					contractText[33]="0";//0.95m
					contractText[34]="0";//1.65m
					contractText[35]="0";//0.9m
					contractText[36]="0";//0.45m
					contractText[37]="0";//0.875m
					}  
				  if(queryResult.getString(1).equals("Sharks"))
					{
					//Allocate UNFL Players to Bayside
					coachNumber=20;
					sql2.executeUpdate("UPDATE players SET TeamNumber="+teamNumber + " "+
									   "WHERE WorldNumber="+leagueNumber + " " +
									   "AND MasterPlayerNumber IN (7645,6635,6515,7866,7223,7353," +
									   "7369,6799,6811,7248,6738,8067,8165," +
									   "7054,6888,7925,6847,7680,6839,7708,6705," +
									   "8363,8573,7752,7718,6905,8020," +
									   "7212,7236,7306,7007,7266,8266," +
									   "7523,7648,8201,6741,8157," +
									   "6978,7581," +
						               "0000,0000,0000,0000,0000" +
									   ")");
					//Set players contracts
					contractText[0]="6839";//0.25m
					contractText[1]="7866,7054,6847";//0.50m
					contractText[2]="6978";//0.75m
					contractText[3]="6811,7925,8363,8573,7007,8157";//1.00m
					contractText[4]="7369,6799,6705,7752,8266,7581";//1.25m
					contractText[5]="6515,7708,7718,7212,7648,8201,7266";//1.50m
					contractText[6]="6738,6888,6905,6741,7306";//1.75m
					contractText[7]="8165,7523";//2.00m
					contractText[8]="8020";//2.25m
					contractText[9]="6635,8067";//2.50m
					contractText[10]="7248";//2.75m
					contractText[11]="7223,7353,7680,7236";//3.00m
					contractText[12]="0";//3.25m
					contractText[13]="0";//3.50m
					contractText[14]="0";//3.75m
					contractText[15]="7645";//4.00m
					contractText[16]="0";//4.25m
					contractText[17]="0";//4.50m
					contractText[18]="0";//4.75m
					contractText[19]="0";//5.00m
					contractText[20]="0";//0.30m
					contractText[21]="0";//0.35m
					contractText[22]="0";//1.20m
					contractText[23]="0";//0.40m
					contractText[24]="0";//0.60m
					contractText[25]="0";//0.80m
					contractText[26]="0";//0.55m
					contractText[27]="0";//0.375m
					contractText[28]="0";//0.625m
					contractText[29]="0";//0.65m
					contractText[30]="0";//1.3m
					contractText[31]="0";//1.1m
					contractText[32]="0";//1.55m
					contractText[33]="0";//0.95m
					contractText[34]="0";//1.65m
					contractText[35]="0";//0.9m
					contractText[36]="0";//0.45m
					contractText[37]="0";//0.875m
                    }					
				 if(queryResult.getString(1).equals("Bengals"))
				   {
				   //Allocate UNFL Players to Cincinnati
				   coachNumber=15;
				   sql2.executeUpdate("UPDATE players SET TeamNumber="+teamNumber + " "+
									  "WHERE WorldNumber="+leagueNumber + " " +
									  "AND MasterPlayerNumber IN (8445,7263,6535,7313,8504,7051," +
									  "7262,6681,8594,7804,6696,6511,6747," +
									  "8470,8124,7528,8357,8229,6702,8581,7490," +
									  "8652,8371,7421,8379,7656,7761," +
									  "7352,7314,8155,7028,8196,8385," +
									  "6481,7721,8127,7005,7130," +
									  "7981,7846," +
						              "0000,0000,0000,0000,0000" +
									  ")");
					//Set players contracts
					contractText[0]="0";//0.25m
					contractText[1]="7313,7804,8124,8652,7981,7846";//0.50m
					contractText[2]="6702,7028,8196,8385,7005,7130";//0.75m
					contractText[3]="6511,8470";//1.00m
					contractText[4]="6696,6747";//1.25m
					contractText[5]="6681,8594,8127";//1.50m
					contractText[6]="7263,6535,7262,7656,7761,7352,8155";//1.75m
					contractText[7]="7051,7528,7490,7421,8379,7314,7721";//2.00m
					contractText[8]="0";//2.25m
					contractText[9]="8504,8229,6481";//2.50m
					contractText[10]="8357";//2.75m
					contractText[11]="8581";//3.00m
					contractText[12]="0";//3.25m
					contractText[13]="8445,8371";//3.50m
					contractText[14]="0";//3.75m
					contractText[15]="0";//4.00m
					contractText[16]="0";//4.25m
					contractText[17]="0";//4.50m
					contractText[18]="0";//4.75m
					contractText[19]="0";//5.00m
					contractText[20]="0";//0.30m
					contractText[21]="0";//0.35m
					contractText[22]="0";//1.20m
					contractText[23]="0";//0.40m
					contractText[24]="0";//0.60m
					contractText[25]="0";//0.80m
					contractText[26]="0";//0.55m
					contractText[27]="0";//0.375m
					contractText[28]="0";//0.625m
					contractText[29]="0";//0.65m
					contractText[30]="0";//1.3m
					contractText[31]="0";//1.1m
					contractText[32]="0";//1.55m
					contractText[33]="0";//0.95m
					contractText[34]="0";//1.65m
					contractText[35]="0";//0.9m
					contractText[36]="0";//0.45m
					contractText[37]="0";//0.875m
                    }
				  if(queryResult.getString(1).equals("Stormbringers"))
					{
					//Allocate UNFL Players to Northampton
					coachNumber=1;
					sql2.executeUpdate("UPDATE players SET TeamNumber="+teamNumber + " "+
									   "WHERE WorldNumber="+leagueNumber + " " +
									   "AND MasterPlayerNumber IN (6588,8192,7589,7719,7653,7950," +
									   "7260,8191,7000,7818,6502,7921,7819," +
									   "6545,7340,6887,7822,6625,6984,8630,6764," +
									   "8589,8261,8071,7100,7463,8002," +
									   "7411,6458,6427,7595,8451,8186," +
									   "8250,8083,6870,7995,8452," +
									   "7564,6526," +
						               "7048,7789,7241,7201,7901" +
									   ")");
					//Set players contracts
					contractText[0]="8192,7819,7901,7822,6427,6526,7048,7789,7241,7201";//0.25m
					contractText[1]="7950,8630,7463,6458";//0.50m
					contractText[2]="7260,8261,8452";//0.75m
					contractText[3]="6887,7100,7995,7564";//1.00m
					contractText[4]="0";//1.25m
					contractText[5]="7921,6625,8250,8083";//1.50m
					contractText[6]="7653,6502";//1.75m
					contractText[7]="7719,6545,7411,8186";//2.00m
					contractText[8]="0";//2.25m
					contractText[9]="7589,7340,6984,8071";//2.50m
					contractText[10]="7818";//2.75m
					contractText[11]="8191,7000,8589,8002,8451,6870";//3.00m
					contractText[12]="0";//3.25m
					contractText[13]="6764,7595";//3.50m
					contractText[14]="0";//3.75m
					contractText[15]="6588";//4.00m
					contractText[16]="0";//4.25m
					contractText[17]="0";//4.50m
					contractText[18]="0";//4.75m
					contractText[19]="0";//5.00m
					contractText[20]="0";//0.30m
					contractText[21]="0";//0.35m
					contractText[22]="0";//1.20m
					contractText[23]="0";//0.40m
					contractText[24]="0";//0.60m
					contractText[25]="0";//0.80m
					contractText[26]="0";//0.55m
					contractText[27]="0";//0.375m
					contractText[28]="0";//0.625m
					contractText[29]="0";//0.65m
					contractText[30]="0";//1.3m
					contractText[31]="0";//1.1m
					contractText[32]="0";//1.55m
					contractText[33]="0";//0.95m
					contractText[34]="0";//1.65m
					contractText[35]="0";//0.9m
					contractText[36]="0";//0.45m
					contractText[37]="0";//0.875m
                    }						
				  if(queryResult.getString(1).equals("Hurricanes"))
					{
					//Allocate UNFL Players to Edinburgh
					coachNumber=13;
					sql2.executeUpdate("UPDATE players SET TeamNumber="+teamNumber + " "+
									   "WHERE WorldNumber="+leagueNumber + " " +
									   "AND MasterPlayerNumber IN (7576,8215,8391,6413,6522,7152," +
									   "8467,7308,6569,6498,6869,7592,8234," +
									   "7383,7018,8390,7095,6626,7892,7384,6462," +
									   "7835,8017,8429,7543,6391,8557," +
									   "7428,6530,8655,8653,7754,6815," +
									   "6816,8336,8114,7987,7688," +
									   "7620,7874," +
									   "7346,7985,6659,6418,6546,8156" +
									   ")");
					//Set players contracts
					contractText[0]="6522,8390,7384,8655,8653,7987,6546";//0.25m
					contractText[1]="7152,7592,6626,6462,6418";//0.50m
					contractText[2]="7018,7543,7346";//0.75m
					contractText[3]="6498,8234,8114,8156";//1.00m
					contractText[4]="7428";//1.25m
					contractText[5]="7095,7835,8557,6530,7874,6659";//1.50m
					contractText[6]="7985";//1.75m
					contractText[7]="6569,6869,7383,7892,6816,7620";//2.00m
					contractText[8]="6815";//2.25m
					contractText[9]="6391,8215,7308,8017,8429,7754,7688";//2.50m
					contractText[10]="7576,8467,8336";//2.75m
					contractText[11]="6413";//3.00m
					contractText[12]="0";//3.25m
					contractText[13]="0";//3.50m
					contractText[14]="0";//3.75m
					contractText[15]="8391";//4.00m
					contractText[16]="0";//4.25m
					contractText[17]="0";//4.50m
					contractText[18]="0";//4.75m
					contractText[19]="0";//5.00m
					contractText[20]="0";//0.30m
					contractText[21]="0";//0.35m
					contractText[22]="0";//1.20m
					contractText[23]="0";//0.40m
					contractText[24]="0";//0.60m
					contractText[25]="0";//0.80m
					contractText[26]="0";//0.55m
					contractText[27]="0";//0.375m
					contractText[28]="0";//0.625m
					contractText[29]="0";//0.65m
					contractText[30]="0";//1.3m
					contractText[31]="0";//1.1m
					contractText[32]="0";//1.55m
					contractText[33]="0";//0.95m
					contractText[34]="0";//1.65m
					contractText[35]="0";//0.9m
					contractText[36]="0";//0.45m
					contractText[37]="0";//0.875m
                    }
				 if(queryResult.getString(1).equals("White Tigers"))
				   {
				   //Allocate UNFL Players to Colorado
				   coachNumber=19;
				   sql2.executeUpdate("UPDATE players SET TeamNumber="+teamNumber + " "+
									  "WHERE WorldNumber="+leagueNumber + " " +
									  "AND MasterPlayerNumber IN (7644,7202,8475,7426,6916,6957," +
									  "7402,6770,8241,7176,8444,6456,6472," +
									  "7521,7087,6694,7886,8197,7980,7460,7638," +
									  "7160,8344,7518,8269,8102,7376," +
									  "8585,8318,8400,6956,8634,7256," +
									  "6783,6734,7989,8642,8322," +
									  "7103,7731,6486,6468,7441," +
									  "8584,7185,6517,7121,7039,7195" +
									  ")");
				   //Set players contracts
				   contractText[0]="6957,8444,6456,6956,6734,7989,7039";//0.25m
				   contractText[1]="8475,7376";//0.50m
				   contractText[2]="6694,7886,7103,8584";//0.75m
				   contractText[3]="8197,7460,7160,8102,6517,7195";//1.00m
				   contractText[4]="6472,7521,8400,7731";//1.25m
				   contractText[5]="7202,8241,7176,7638";//1.50m
				   contractText[6]="7121";//1.75m
				   contractText[7]="6916,7980,8269,7256,8322,7185";//2.00m
				   contractText[8]="8634,8642";//2.25m
				   contractText[9]="7426,6770,7087,7518,8318,6783";//2.50m
				   contractText[10]="0";//2.75m
				   contractText[11]="7644,7402,8344,8585";//3.00m
				   contractText[12]="0";//3.25m
				   contractText[13]="0";//3.50m
				   contractText[14]="0";//3.75m
				   contractText[15]="0";//4.00m
				   contractText[16]="0";//4.25m
				   contractText[17]="0";//4.50m
				   contractText[18]="0";//4.75m
				   contractText[19]="0";//5.00m
				   contractText[20]="7441";//0.30m
				   contractText[21]="6486";//0.35m
				   contractText[22]="6468";//1.20m
				   contractText[23]="0";//0.40m
				   contractText[24]="0";//0.60m
				   contractText[25]="0";//0.80m
				   contractText[26]="0";//0.55m
				   contractText[27]="0";//0.375m
				   contractText[28]="0";//0.625m
				   contractText[29]="0";//0.65m
				   contractText[30]="0";//1.3m
				   contractText[31]="0";//1.1m
				   contractText[32]="0";//1.55m
				   contractText[33]="0";//0.95m
				   contractText[34]="0";//1.65m
				   contractText[35]="0";//0.9m
				   contractText[36]="0";//0.45m
				   contractText[37]="0";//0.875m
                   }					
				if(queryResult.getString(1).equals("Rams"))
				   {
				   //Allocate UNFL Players to Purdue
				   coachNumber=3;
				   sql2.executeUpdate("UPDATE players SET TeamNumber="+teamNumber + " "+
									  "WHERE WorldNumber="+leagueNumber + " " +
									  "AND MasterPlayerNumber IN (7743,7614,7364,7869,7974,8506," +
									  "6425,8243,7318,7419,6827,8497,7739," +
									  "8647,8153,6490,7763,7098,8461,8251,7527," +
									  "8140,8108,7599,6617,8511,6850," +
									  "6955,8139,8464,8659,6464,6612," +
									  "6731,8359,8403,8209,7243," +
									  "7261,7359,6564,8476,8612," +
									  "7282,6618,6493,8218,6697,7982" +
									  ")");
				   //Set players contracts
				   contractText[0]="8108";//0.25m
				   contractText[1]="7974,8506,7098,7527,6850,6464,6493,7982";//0.50m
				   contractText[2]="8511,8659,6731";//0.75m
				   contractText[3]="7869,6697";//1.00m
				   contractText[4]="6425,8497,8647,8251,8209,6564";//1.25m
				   contractText[5]="7614,7359";//1.50m
				   contractText[6]="6827,6490,7763,6617,8464,7243,7261,7282,6618";//1.75m
				   contractText[7]="7419,7739,8461,8139,8612";//2.00m
				   contractText[8]="8403";//2.25m
				   contractText[9]="7743,7599";//2.50m
				   contractText[10]="7318";//2.75m
				   contractText[11]="7364,8243,8140,6612";//3.00m
				   contractText[12]="0";//3.25m
				   contractText[13]="0";//3.50m
				   contractText[14]="0";//3.75m
				   contractText[15]="0";//4.00m
				   contractText[16]="0";//4.25m
				   contractText[17]="0";//4.50m
				   contractText[18]="0";//4.75m
				   contractText[19]="0";//5.00m
				   contractText[20]="8218";//0.30m
				   contractText[21]="0";//0.35m
				   contractText[22]="0";//1.20m
				   contractText[23]="8359,8476";//0.40m
				   contractText[24]="6955";//0.60m
				   contractText[25]="8153";//0.80m
				   contractText[26]="0";//0.55m
				   contractText[27]="0";//0.375m
				   contractText[28]="0";//0.625m
				   contractText[29]="0";//0.65m
				   contractText[30]="0";//1.3m
				   contractText[31]="0";//1.1m
				   contractText[32]="0";//1.55m
				   contractText[33]="0";//0.95m
				   contractText[34]="0";//1.65m
				   contractText[35]="0";//0.9m
				   contractText[36]="0";//0.45m
				   contractText[37]="0";//0.875m
                   }						
				 if(queryResult.getString(1).equals("Bullets"))
				   {
				   //Allocate UNFL Players to Boston
				   coachNumber=9;
				   sql2.executeUpdate("UPDATE players SET TeamNumber="+teamNumber + " "+
									  "WHERE WorldNumber="+leagueNumber + " " +
									  "AND MasterPlayerNumber IN (7059,6829,8082,7580,7456,8422," +
									  "8535,7420,6959,6744,6640,6592,7351," +
									  "8310,7116,7998,6487,8299,7469,6780,8213," +
									  "8366,8205,7751,7888,7173,8162," +
									  "7132,8106,8592,7856,7246,6896," +
									  "8434,7970,6409,8485,6837,6627," +
									  "7049,7097,7141,8641,7177,6556," +
									  "6453,7461,7290,7934,6454,6533,7684" +
									  ")");
				   //Set players contracts
				   contractText[0]="7580,8310,7998,6780,8205,8106,6409,7141,8641,7934,6556";//0.25m
				   contractText[1]="7420,7351,7856";//0.50m
				   contractText[2]="7469,7049,6627";//0.75m
				   contractText[3]="8213,7461";//1.00m
				   contractText[4]="8535,6487,8299,7290";//1.25m
				   contractText[5]="8422,7116,8162,7132,7246,6453,6454";//1.50m
				   contractText[6]="7059,6744,7097,6533";//1.75m
				   contractText[7]="7456,8366,7173,8592,6896,6837,7177";//2.00m
				   contractText[8]="6959,7970";//2.25m
				   contractText[9]="6829,6640,6592,7751,8485";//2.50m
				   contractText[10]="0";//2.75m
				   contractText[11]="0";//3.00m
				   contractText[12]="0";//3.25m
				   contractText[13]="8082,8434";//3.50m
				   contractText[14]="0";//3.75m
				   contractText[15]="0";//4.00m
				   contractText[16]="0";//4.25m
				   contractText[17]="0";//4.50m
				   contractText[18]="0";//4.75m
				   contractText[19]="0";//5.00m
				   contractText[20]="7684";//0.30m
				   contractText[21]="0";//0.35m
				   contractText[22]="0";//1.20m
				   contractText[23]="0";//0.40m
				   contractText[24]="0";//0.60m
				   contractText[25]="0";//0.80m
				   contractText[26]="7888";//0.55m
				   contractText[27]="0";//0.375m
				   contractText[28]="0";//0.625m
				   contractText[29]="0";//0.65m
				   contractText[30]="0";//1.3m
				   contractText[31]="0";//1.1m
				   contractText[32]="0";//1.55m
				   contractText[33]="0";//0.95m
				   contractText[34]="0";//1.65m
				   contractText[35]="0";//0.9m
				   contractText[36]="0";//0.45m
				   contractText[37]="0";//0.875m
                   }				
				 if(queryResult.getString(1).equals("King Squad"))
				   {
				   //Allocate UNFL Players to Berlin
				   coachNumber=14;
				   sql2.executeUpdate("UPDATE players SET TeamNumber="+teamNumber + " "+
									  "WHERE WorldNumber="+leagueNumber + " " +
									  "AND MasterPlayerNumber IN (7742,6500,6817,6671,6523,7936," +
									  "8331,8232,8104,6972,7649,7408,7311," +
									  "7449,6825,7407,8425,7651,7786,7670,7630," +
									  "8178,7210,8590,6965,8459,8437," +
									  "7071,7072,8219,7389,6769,8287," +
									  "8255,8203,7040,6867,7169,7350," +
									  "6609,8597,7643,8375,7381,7646," +
									  "8343,8660,7395,7115,8311" +
									  ")");
				  //Set players contracts
				  contractText[0]="6817,7936,8219,8255,6609";//0.25m
				  contractText[1]="8331,7350,7395,7115";//0.50m
				  contractText[2]="8104,8459,8660";//0.75m
				  contractText[3]="7670,8311";//1.00m
				  contractText[4]="6500,8287,7643";//1.25m
				  contractText[5]="6825,7407,7651,7786,7210,8590,7169,8375,8343";//1.50m
				  contractText[6]="8178,7072,8203,7040,6867,7381,7646";//1.75m
				  contractText[7]="6523,8232,7449,6965,8437,7071,7389,6769";//2.00m
				  contractText[8]="0";//2.25m
				  contractText[9]="8597";//2.50m
				  contractText[10]="0";//2.75m
				  contractText[11]="7311";//3.00m
				  contractText[12]="0";//3.25m
				  contractText[13]="7742,6671,7408";//3.50m
				  contractText[14]="0";//3.75m
				  contractText[15]="0";//4.00m
				  contractText[16]="0";//4.25m
				  contractText[17]="0";//4.50m
				  contractText[18]="0";//4.75m
				  contractText[19]="0";//5.00m
				  contractText[20]="0";//0.30m
				  contractText[21]="0";//0.35m
				  contractText[22]="0";//1.20m
				  contractText[23]="0";//0.40m
				  contractText[24]="7630";//0.60m
				  contractText[25]="0";//0.80m
				  contractText[26]="0";//0.55m
				  contractText[27]="6972,7649";//0.375m
				  contractText[28]="8425";//0.625m
				  contractText[29]="0";//0.65m
				  contractText[30]="0";//1.3m
				  contractText[31]="0";//1.1m
				  contractText[32]="0";//1.55m
				  contractText[33]="0";//0.95m
				  contractText[34]="0";//1.65m
				  contractText[35]="0";//0.9m
				  contractText[36]="0";//0.45m
				  contractText[37]="0";//0.875m
                  }	
				if(queryResult.getString(1).equals("Hawks"))
				  {
				  //Allocate UNFL Players to Iowa
				  coachNumber=18;
				  sql2.executeUpdate("UPDATE players SET TeamNumber="+teamNumber + " "+
									 "WHERE WorldNumber="+leagueNumber + " " +
									 "AND MasterPlayerNumber IN (7913,7159,8339,8576,6974,8317," +
									 "8484,6802,6531,7316,7046,6455,8636," +
									 "7520,7455,8110,7056,6547,8501,7575,6442," +
									 "6735,7358,7025,6663,7491,8666," +
									 "7484,7144,6985,8586,7387,6602," +
									 "8268,7705,7563,6698,7812,7810," +
									 "8587,8087,7200,6624,6881,8516," +
									 "8225,8502,7768,7326,7652,7213" +
									 ")");
				  //Set players contracts
				  contractText[0]="6455,6735,6698,8587,7213";//0.25m
				  contractText[1]="8317,6547,8516,8502,7652";//0.50m
				  contractText[2]="6974,7387,7705,6624,6881";//0.75m
				  contractText[3]="7520,7056,7358";//1.00m
				  contractText[4]="7025,8586,7563";//1.25m
				  contractText[5]="8576,7575,7484,8268,7326";//1.50m
				  contractText[6]="7491,8087,7200,8225";//1.75m
				  contractText[7]="7159,8339,8110,8501,6663,7144";//2.00m
				  contractText[8]="6802,8636";//2.25m
				  contractText[9]="8484";//2.50m
				  contractText[10]="6531,7812";//2.75m
				  contractText[11]="7455,6602";//3.00m
				  contractText[12]="0";//3.25m
				  contractText[13]="7913";//3.50m
				  contractText[14]="0";//3.75m
				  contractText[15]="0";//4.00m
				  contractText[16]="0";//4.25m
				  contractText[17]="0";//4.50m
				  contractText[18]="0";//4.75m
				  contractText[19]="0";//5.00m
				  contractText[20]="0";//0.30m
				  contractText[21]="0";//0.35m
				  contractText[22]="6985";//1.20m
				  contractText[23]="0";//0.40m
				  contractText[24]="0";//0.60m
				  contractText[25]="0";//0.80m
				  contractText[26]="0";//0.55m
				  contractText[27]="0";//0.375m
				  contractText[28]="0";//0.625m
				  contractText[29]="7046,7810";//0.65m
				  contractText[30]="7316,8666";//1.3m
				  contractText[31]="6442";//1.1m
				  contractText[32]="7768";//1.55m
				  contractText[33]="0";//0.95m
				  contractText[34]="0";//1.65m
				  contractText[35]="0";//0.9m
				  contractText[36]="0";//0.45m
				  contractText[37]="0";//0.875m
				  }	 
				if(queryResult.getString(1).equals("Mounties"))
				  {
				  //Allocate UNFL Players to Montreal
				  coachNumber=8;
				  sql2.executeUpdate("UPDATE players SET TeamNumber="+teamNumber + " "+
									 "WHERE WorldNumber="+leagueNumber + " " +
									 "AND MasterPlayerNumber IN (7232,6686,8004,7811,8340,7823," +
									 "7417,6810,7666,6706,8577,7694,7438," +
									 "8654,8143,7415,7294,7960,7746,7603,7945," +
									 "7791,7267,7494,6772,8264,7075," +
									 "7252,7946,7481,6719,6722,7969," +
									 "7320,7833,7942,8355,6495,6566," +
									 "7385,7959,7374,7451,8407,7283," +
									 "8492,7196,8129,8088,7472,7225,7118,8115" +
									 ")");
				  //Set players contracts
				  contractText[0]="8340,7417,8654,7294,7252";//0.25m
				  contractText[1]="7823,6772,6722,7196";//0.50m
				  contractText[2]="7811,8577,7694,7438,7415,7945,7385,8492";//0.75m
				  contractText[3]="6706,8143,7603,7791,7267,7494,7075,7942";//1.00m
				  contractText[4]="7746,8264,7283,7225";//1.25m
				  contractText[5]="6686,7666,7320,7833,6566,8129,8088,7472";//1.50m
				  contractText[6]="6810,7960,6719";//1.75m
				  contractText[7]="8004,7481,7969,6495,7374,7451,8407";//2.00m
				  contractText[8]="8355,7959";//2.25m
				  contractText[9]="7232";//2.50m
				  contractText[10]="0";//2.75m
				  contractText[11]="0";//3.00m
				  contractText[12]="0";//3.25m
				  contractText[13]="7946";//3.50m
				  contractText[14]="0";//3.75m
				  contractText[15]="0";//4.00m
				  contractText[16]="0";//4.25m
				  contractText[17]="0";//4.50m
				  contractText[18]="0";//4.75m
				  contractText[19]="0";//5.00m
				  contractText[20]="0";//0.30m
				  contractText[21]="0";//0.35m
				  contractText[22]="0";//1.20m
				  contractText[23]="7118,8115";//0.40m
				  contractText[24]="0";//0.60m
				  contractText[25]="0";//0.80m
				  contractText[26]="0";//0.55m
				  contractText[27]="0";//0.375m
				  contractText[28]="0";//0.625m
				  contractText[29]="0";//0.65m
				  contractText[30]="0";//1.3m
				  contractText[31]="0";//1.1m
				  contractText[32]="0";//1.55m
				  contractText[33]="0";//0.95m
				  contractText[34]="0";//1.65m
				  contractText[35]="0";//0.9m
				  contractText[36]="0";//0.45m
				  contractText[37]="0";//0.875m
				  }
				if(queryResult.getString(1).equals("Removers"))
				  {
				  //Allocate UNFL Players to Staines
				  coachNumber=10;
				  sql2.executeUpdate("UPDATE players SET TeamNumber="+teamNumber + " "+
									 "WHERE WorldNumber="+leagueNumber + " " +
									 "AND MasterPlayerNumber IN (8090,7065,8146,8529,7375,6873," +
									 "7732,6668,7088,8032,7916,7918,6818," +
									 "7250,8128,6396,7726,7834,8291,6573,7019," +
									 "7188,8214,6940,6929,8500,7495," +
									 "7357,7967,7465,6942,6926,8117," +
									 "7022,8167,8628,6919,8364,8131," +
									 "7966,7724,7310,6398,7113,7551" +
									 ")");
				  //Set players contracts
				  contractText[0]="7732";//0.25m
				  contractText[1]="8146,8131,7551";//0.50m
				  contractText[2]="6873,7918,7726,6929,7022,8364,7724,6398";//0.75m
				  contractText[3]="7250,6573,7019,7188,7495,6942,8167,7113";//1.00m
				  contractText[4]="0";//1.25m
				  contractText[5]="7916,7967,7310";//1.50m
				  contractText[6]="8032,8500,7966";//1.75m
				  contractText[7]="7065,7375,8128,6396,7834,8291,8214,6919";//2.00m
				  contractText[8]="6940,8628";//2.25m
				  contractText[9]="8090,7088,6818,7465";//2.50m
				  contractText[10]="0";//2.75m
				  contractText[11]="8529,6668,8117";//3.00m
				  contractText[12]="0";//3.25m
				  contractText[13]="6926";//3.50m
				  contractText[14]="0";//3.75m
				  contractText[15]="0";//4.00m
				  contractText[16]="0";//4.25m
				  contractText[17]="0";//4.50m
				  contractText[18]="0";//4.75m
				  contractText[19]="0";//5.00m
				  contractText[20]="0";//0.30m
				  contractText[21]="0";//0.35m
				  contractText[22]="0";//1.20m
				  contractText[23]="0";//0.40m
				  contractText[24]="0";//0.60m
				  contractText[25]="0";//0.80m
				  contractText[26]="0";//0.55m
				  contractText[27]="0";//0.375m
				  contractText[28]="0";//0.625m
				  contractText[29]="0";//0.65m
				  contractText[30]="0";//1.3m
				  contractText[31]="0";//1.1m
				  contractText[32]="0";//1.55m
				  contractText[33]="0";//0.95m
				  contractText[34]="0";//1.65m
				  contractText[35]="0";//0.9m
				  contractText[36]="0";//0.45m
				  contractText[37]="7357";//0.875m
				  }			
				if(queryResult.getString(1).equals("Mutton Birds"))
				  {
				  //Allocate UNFL Players to Lord Howe
				  coachNumber=6;
				  sql2.executeUpdate("UPDATE players SET TeamNumber="+teamNumber + " "+
									 "WHERE WorldNumber="+leagueNumber + " " +
									 "AND MasterPlayerNumber IN (8668,6701,6401,7445,6992,8057," +
									 "6782,7164,7820,8346,8537,7764,8435," +
									 "6431,8091,8406,8306,6958,6894,6631,8524," +
									 "8118,6913,7887,8267,6768,6759," +
									 "6605,6838,6762,6845,8624,6583," +
									 "7279,8200,7302,7657,8384,7024," +
									 "8101,8622,8545,7549,6658,0000," +
									 "8130,8510,7112" +
									 ")");
				  //Set players contracts
				  contractText[0]="8118,8622";//0.25m
				  contractText[1]="6992,6768,8130,8510,7112";//0.50m
				  contractText[2]="8537,6762,8101";//0.75m
				  contractText[3]="8435,8306,6631,6913";//1.00m
				  contractText[4]="8057,6431,6958,8524,8267,6759,8624,8545";//1.25m
				  contractText[5]="7164,8091,6894,6838,8200,7657,7024,6658";//1.50m
				  contractText[6]="7820,8406,6605,7549";//1.75m
				  contractText[7]="6701,7445,8346,7764,7887,6845,7279";//2.00m
				  contractText[8]="6583";//2.25m
				  contractText[9]="6401,8384";//2.50m
				  contractText[10]="6782";//2.75m
				  contractText[11]="8668";//3.00m
				  contractText[12]="0";//3.25m
				  contractText[13]="0";//3.50m
				  contractText[14]="0";//3.75m
				  contractText[15]="0";//4.00m
				  contractText[16]="0";//4.25m
				  contractText[17]="0";//4.50m
				  contractText[18]="0";//4.75m
				  contractText[19]="0";//5.00m
				  contractText[20]="0";//0.30m
				  contractText[21]="0";//0.35m
				  contractText[22]="0";//1.20m
				  contractText[23]="0";//0.40m
				  contractText[24]="0";//0.60m
				  contractText[25]="0";//0.80m
				  contractText[26]="0";//0.55m
				  contractText[27]="0";//0.375m
				  contractText[28]="0";//0.625m
				  contractText[29]="0";//0.65m
				  contractText[30]="0";//1.3m
				  contractText[31]="0";//1.1m
				  contractText[32]="0";//1.55m
				  contractText[33]="7302";//0.95m
				  contractText[34]="0";//1.65m
				  contractText[35]="0";//0.9m
				  contractText[36]="0";//0.45m
				  contractText[37]="0";//0.875m
				  }	
				 if(queryResult.getString(1).equals("Jedi"))
				  {
				  //Allocate UNFL Players to San Francisco
				  coachNumber=12;
				  sql2.executeUpdate("UPDATE players SET TeamNumber="+teamNumber + " "+
									 "WHERE WorldNumber="+leagueNumber + " " +
									 "AND MasterPlayerNumber IN (6878,7366,7110,8570,8495,7476," +
									 "7174,8147,8216,8358,8508,8598,6646," +
									 "7654,7664,8563,7596,8370,6466,6601,6886," +
									 "7055,8453,7198,7009,6417,6934," +
									 "6995,8332,6402,8041,7931,8134," +
									 "8380,8159,7322,7142,7554,7285," +
									 "6479,8271,8086,7052,8569,7149," +
									 "8058,6776,8617,7911,7871,6473" +
									 ")");
				  //Set players contracts
				  contractText[0]="7366,7476,8358,6473";//0.25m
				  contractText[1]="7174,8147,7654,8563";//0.50m
				  contractText[2]="7664,6601,8332,6479,7149,7871";//0.75m
				  contractText[3]="8453,6402,8086,7285";//1.00m
				  contractText[4]="8134,8380,7554";//1.25m
				  contractText[5]="6646,6466,6886,7055,7009,6995,7322,7052";//1.50m
				  contractText[6]="8495,7596,6934,8041,8617";//1.75m
				  contractText[7]="8370,7198,6417,7931,7142,6776";//2.00m
				  contractText[8]="0";//2.25m
				  contractText[9]="8058";//2.50m
				  contractText[10]="6878,8159";//2.75m
				  contractText[11]="7110,8598";//3.00m
				  contractText[12]="0";//3.25m
				  contractText[13]="8508";//3.50m
				  contractText[14]="0";//3.75m
				  contractText[15]="0";//4.00m
				  contractText[16]="0";//4.25m
				  contractText[17]="0";//4.50m
				  contractText[18]="0";//4.75m
				  contractText[19]="0";//5.00m
				  contractText[20]="0";//0.30m
				  contractText[21]="0";//0.35m
				  contractText[22]="0";//1.20m
				  contractText[23]="0";//0.40m
				  contractText[24]="8271";//0.60m
				  contractText[25]="0";//0.80m
				  contractText[26]="0";//0.55m
				  contractText[27]="0";//0.375m
				  contractText[28]="0";//0.625m
				  contractText[29]="0";//0.65m
				  contractText[30]="0";//1.3m
				  contractText[31]="0";//1.1m
				  contractText[32]="0";//1.55m
				  contractText[33]="0";//0.95m
				  contractText[34]="8570";//1.65m
				  contractText[35]="8216,8569";//0.9m
				  contractText[36]="7911";//0.45m
				  contractText[37]="0";//0.875m
				  }				  					  	  				                   			 						   			   																				                   					                     
				if(queryResult.getString(1).equals("Tigers"))
				  {
				  //Allocate UNFL Players to Tyrone
				  coachNumber=5;
				  sql2.executeUpdate("UPDATE players SET TeamNumber="+teamNumber + " "+
									 "WHERE WorldNumber="+leagueNumber + " " +
									 "AND MasterPlayerNumber IN (7944,6718,7895,6483,6516,8236," +
									 "6452,7668,6906,6565,6723,7867,7166," +
									 "8122,8676,6496,7537,7401,7873,7640,7182," +
									 "6765,6438,7954,8330,7258,7089," +
									 "8488,7891,7120,7885,6693,6970," +
									 "6798,7228,8574,7440,8000,6751," +
									 "7673,7553,8075,8399,7996,8006," +
									 "8678,8126,7773,6728,6712,8432,6936,6781" +
									 ")");
				  //Set players contracts
				  contractText[0]="7895,8122,6496,7996,8006,8678,8432";//0.25m
				  contractText[1]="8236,7668,6693,6781";//0.50m
				  contractText[2]="6718,6452,7166,6798,8126";//0.75m
				  contractText[3]="7401,8000,8399,6712,6936";//1.00m
				  contractText[4]="6765,7954,8488";//1.25m
				  contractText[5]="7867,7182,7440";//1.50m
				  contractText[6]="6516,8330,7673,6728";//1.75m
				  contractText[7]="6723,7640,7258,7773";//2.00m
				  contractText[8]="6970";//2.25m
				  contractText[9]="6438,7120,7885,8574";//2.50m
				  contractText[10]="7944,7553,8075";//2.75m
				  contractText[11]="6483,6906,6565,7537,7089";//3.00m
				  contractText[12]="0";//3.25m
				  contractText[13]="0";//3.50m
				  contractText[14]="0";//3.75m
				  contractText[15]="0";//4.00m
				  contractText[16]="0";//4.25m
				  contractText[17]="0";//4.50m
				  contractText[18]="0";//4.75m
				  contractText[19]="0";//5.00m
				  contractText[20]="0";//0.30m
				  contractText[21]="0";//0.35m
				  contractText[22]="0";//1.20m
				  contractText[23]="8676";//0.40m
				  contractText[24]="0";//0.60m
				  contractText[25]="7873";//0.80m
				  contractText[26]="0";//0.55m
				  contractText[27]="0";//0.375m
				  contractText[28]="7891,7228,6751";//0.625m
				  contractText[29]="0";//0.65m
				  contractText[30]="0";//1.3m
				  contractText[31]="0";//1.1m
				  contractText[32]="0";//1.55m
				  contractText[33]="0";//0.95m
				  contractText[34]="0";//1.65m
				  contractText[35]="0";//0.9m
				  contractText[36]="0";//0.45m
				  contractText[37]="0";//0.875m
				  }
                 for(int currentValue=0;currentValue<contractValues.length;currentValue++)
                    {                  
					sql2.executeUpdate("UPDATE players SET ContractValue = " +contractValues[currentValue] + " " +
					                   ",ContractLength=3 " +
					                   "WHERE TeamNumber=" + teamNumber + " " +
                                       "AND MasterPlayerNumber IN ("+ contractText[currentValue] +
							           ")");
                    }				                         
				 sql2.executeUpdate("INSERT INTO coachteams (" +
									"TeamNumber,CoachNumber,DateTimeStamp)" +
									" VALUES (" +
									teamNumber +
									"," +
									coachNumber +
									",'" +
									Routines.getDateTime(false) +
									"')");    
                 ResultSet queryResult2=sql2.executeQuery("SELECT players.PlayerNumber,PreferredNumber,ShirtMin1,ShirtMax1,ShirtMin2,ShirtMax2 " +
                                                          "FROM players,masterplayers,positions " +
                                                          "WHERE TeamNumber=" + teamNumber + " " +                                                          "AND players.MasterPlayerNumber=masterplayers.MasterPlayerNumber " +
                                                          "AND masterplayers.PositionNumber=positions.PositionNumber " +
                                                          "ORDER BY PlayerNumber ASC");
                 while(queryResult2.next())
                      {
                      //Get each player to allocate a shirt number too.	
                      int shirtNumber=0;	
                      int playerNumber=queryResult2.getInt(1);
                      int preferredNumber=queryResult2.getInt(2);
                      int shirtMin1=queryResult2.getInt(3);
					  int shirtMax1=queryResult2.getInt(4);
					  int shirtMin2=queryResult2.getInt(5);
					  int shirtMax2=queryResult2.getInt(6);
					  if(!shirt[preferredNumber])
					    {
					    //Allocate their preferred number if possible.	
					    shirtNumber=preferredNumber;
						shirt[shirtNumber]=true;		                                      	
					    }
					  else
					    {
						//Allocate a shirt number.	
					    Vector shirtNumbers= new Vector();
					    int numOfShirts=0;	
					    for(int currentRange=0;currentRange<2&&shirtNumber==0;currentRange++)
					       {
					       int shirtMin=0;
					       int shirtMax=0;	
					       //Set Range to search on this iteration.
					       if(currentRange==0)
					         {
					         shirtMin=shirtMin1;
					         shirtMax=shirtMax1;	
					         }		
					       else
					         {
					         shirtMin=shirtMin2;
					         shirtMax=shirtMax2;	
					         }	  
					       for(int currentShirt=shirtMin;currentShirt<=shirtMax;currentShirt++)
					         {		  
					         //Load up Vector with all shirt numbers from current range.	
					         if(!shirt[currentShirt])
					           {
					           numOfShirts++;
							   shirtNumbers.setSize(numOfShirts);
							   Object shirtString=new String("");
							   shirtString=String.valueOf(currentShirt);
							   shirtNumbers.insertElementAt(shirtString,numOfShirts);
					           }
					         }
 					       for(int currentShirt=1;currentShirt<shirtNumbers.size();currentShirt++)
					         {   
					         //Remove shirts that have already been allocated.	
					         Object objShirt=shirtNumbers.get(currentShirt);
					         String strShirt=objShirt.toString();
					         int intShirt=Routines.safeParseInt(strShirt);
					         if(shirt[intShirt])
					           {
					           shirtNumbers.remove(currentShirt);
					           shirtNumbers.trimToSize();		
					           }
					         }
					       }//End of currentRange loop. 
					     //Allocate a shirt from remaining range.
					     int range=shirtNumbers.size();
						 Random random = new Random(playerNumber);
						 shirtNumber=Routines.safeParseInt((String)shirtNumbers.get(((int)(random.nextDouble()*range))));
						 shirt[shirtNumber]=true;
						 }//End of Else section.
					  int updated=sql3.executeUpdate("UPDATE players SET Number="+shirtNumber + " "+
										             "WHERE PlayerNumber="+playerNumber);
                      if(updated!=1)
                        { 
						Routines.writeToLog(servletName,"Shirt("+shirtNumber+") not updated for player(" + playerNumber + ")",false,context);
                        }
                      }  
                 }
               }
             }
          else
             {   
			 //Create DraftBoard
			 int updated=sql1.executeUpdate("INSERT INTO draftboard (" +
			 							    "LeagueNumber,DateTimeStamp) " +
										    "VALUES (" +
										    leagueNumber + ",'" +
										    Routines.getDateTime(false) + "')");
			 if(updated!=1)
			  {
              Routines.writeToLog(servletName,"draftboard not created",false,context);
			  }
		     //Create random team rankings
		     queryResponse1=sql1.executeQuery("SELECT COUNT(TeamNumber) " +
											  "FROM leagueteams,divisions,conferences " +
											  "WHERE leagueteams.DivisionNumber = divisions.DivisionNumber " +
											  "AND divisions.ConferenceNumber=conferences.ConferenceNumber " +
											  "AND conferences.LeagueNumber=" + leagueNumber);
			 queryResponse1.first();
			 int numOfTeams=queryResponse1.getInt(1);
			 Random random = new Random(leagueNumber);
			 boolean rankSelected[]=new boolean[numOfTeams];
			 int ranking[]=new int[numOfTeams];
			 for(int currentRank=0;currentRank<numOfTeams;currentRank++)
			    {
			    boolean rankSet=false;
			    int randomNumber=((int)(random.nextDouble()*(numOfTeams)));
			    for(int currentAttempt=0;currentAttempt<numOfTeams&&!rankSet;currentAttempt++)
			 	  {
			 	  if(randomNumber+currentAttempt>(numOfTeams-1))
			 		{
			 		randomNumber=0-currentAttempt;
			 		}
			 	  if(!rankSelected[randomNumber+currentAttempt])
			 		{
			 		ranking[currentRank]=randomNumber+currentAttempt;
			 		rankSelected[randomNumber+currentAttempt]=true;
			 		rankSet=true;
			 		}
			 	  }
			 	}
			//Assign rankings to teams and create setup draft order
			queryResponse1=sql1.executeQuery("SELECT TeamNumber " +
											 "FROM leagueteams,divisions,conferences " +
											 "WHERE leagueteams.DivisionNumber = divisions.DivisionNumber " +
											 "AND divisions.ConferenceNumber=conferences.ConferenceNumber " +
											 "AND conferences.LeagueNumber=" + leagueNumber);
			int currentRank=0;
			while(queryResponse1.next())
			     {
			     teamNumber=queryResponse1.getInt(1);
				 updated=sql2.executeUpdate("INSERT INTO draftboardteam (" +
					   				        "LeagueNumber,TeamNumber," +
											"Sequence,DateTimeStamp) " +
											"VALUES (" +
											leagueNumber + "," +
											teamNumber + "," +
											(ranking[currentRank]+1) + ",'" +
											Routines.getDateTime(false) + "')");
				 if(updated!=1)
				   {
				   Routines.writeToLog(servletName,"draftboardteam not created (TeamNumber=" +
									   teamNumber +
									   ")",false,context);
				   }
				 //
				 currentRank++;
				 }
			if(!Routines.createDraftRatings(leagueNumber,
											0,
											false,
											null,
											database,
											context))
			   {
			   Routines.writeToLog(servletName,"createDraftRatings failed (Default)",false,context);
			   } 
            }

          //Set league status for viewing
          int updated=sql1.executeUpdate("UPDATE leagues " +
                                     "SET Status=1 " +
                                     "WHERE LeagueNumber=" + leagueNumber);
          if(updated!=1)
            {
            Routines.writeToLog(servletName,"League not updated (LeagueNumber=" +
                                           leagueNumber +
                                           ")",false,context);
            }
          try
            {
            response.sendRedirect(Routines.getRedirect(request,response,context));
            }
          catch(IOException error)
            {
            Routines.writeToLog(servletName,"Error redirecting(2) : " + error,false,context);
            }
          return;
          }
        catch(SQLException error)
          {
          session.setAttribute("message",error.getMessage());
          }
        }
      }

      Routines.WriteHTMLHead("Create League",//title
                             false,//showMenu
                             13,//menuHighLight
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
      pool.returnConnection(database);
      webPageOutput.println("<CENTER>");
      webPageOutput.println("<IMG SRC=\"../Images/EnterData.gif\"" +
                            " WIDTH='256' HEIGHT='40' ALT='Enter Data'>");
      webPageOutput.println("</CENTER>");
      Routines.tableStart(false,webPageOutput);
      Routines.tableHeader("Enter details for new league",0,webPageOutput);
      Routines.tableDataStart(true,false,true,true,true,0,0,"scoresrow",webPageOutput);
      webPageOutput.println("<FORM ACTION=\"http://" +
                             request.getServerName() +
                             ":" +
                             request.getServerPort() +
                             request.getContextPath() +
                             "/servlet/CreateLeague\" METHOD=\"POST\">");
      Routines.messageCheck(true,request,webPageOutput);
      Routines.tableStart(false,webPageOutput);
      Routines.tableDataStart(true,false,true,true,true,titleWidth,0,"scoresrow",webPageOutput);
      webPageOutput.println("League Type");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,inputWidth,0,"scoresrow",webPageOutput);
      webPageOutput.println("<SELECT NAME=\"type\">");
      webPageOutput.println("<OPTION VALUE=\"1\">NFL");
	  webPageOutput.println("<OPTION VALUE=\"2\">UNFL");
      webPageOutput.println("</SELECT>");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableDataStart(true,false,true,true,true,titleWidth,0,"scoresrow",webPageOutput);
      webPageOutput.println("Code Base");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,inputWidth,0,"scoresrow",webPageOutput);
      webPageOutput.println("<SELECT NAME=\"code\">");
      webPageOutput.println("<OPTION VALUE=\"1\">Alpha");
      webPageOutput.println("<OPTION VALUE=\"2\">Beta");
      webPageOutput.println("<OPTION SELECTED VALUE=\"3\">Live");
      webPageOutput.println("</SELECT>");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableDataStart(true,false,true,true,true,titleWidth,0,"scoresrow",webPageOutput);
      webPageOutput.println("Difficulty Level");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,inputWidth,0,"scoresrow",webPageOutput);
      webPageOutput.println("<SELECT NAME=\"level\">");
      webPageOutput.println("<OPTION VALUE=\"1\">Basic");
      webPageOutput.println("<OPTION VALUE=\"2\">Standard");
      webPageOutput.println("<OPTION VALUE=\"3\">Advanced");
	  webPageOutput.println("<OPTION SELECTED VALUE=\"4\">Mixed");
      webPageOutput.println("</SELECT>");
      Routines.tableDataEnd(true,false,true,webPageOutput);
      Routines.tableDataStart(true,false,true,true,true,titleWidth,0,"scoresrow",webPageOutput);
      webPageOutput.println("League Name");
      Routines.tableDataEnd(true,false,false,webPageOutput);
      Routines.tableDataStart(true,false,true,false,false,inputWidth,0,"scoresrow",webPageOutput);
      webPageOutput.println("<INPUT TYPE=\"TEXT\" NAME=\"leagueName\" SIZE=\"20\" MAXLENGTH=\"50\">");
      Routines.tableDataEnd(true,true,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(2));
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Create League\" NAME=\"action\">");
      webPageOutput.println("<INPUT TYPE=\"SUBMIT\" VALUE=\"Cancel\" NAME=\"action\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"action\" VALUE=\"viewMainAdminMenu\">");
      webPageOutput.println("<INPUT TYPE=\"hidden\" NAME=\"jsessionid\" VALUE=\"" + session.getId() + "\">");
      webPageOutput.println("</FORM>");
      Routines.tableDataEnd(true,true,true,webPageOutput);
      Routines.tableEnd(webPageOutput);
      webPageOutput.println(Routines.spaceLines(18));
      Routines.WriteHTMLTail(request,response,webPageOutput);
      }
   }