// Process Results

import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class ProcessResults extends HttpServlet
   {
   private ConnectionPool pool;
   private static String servletName="ProcessResults";
   // Declare & Init "Best" tables.
   static int awayTeamPasser[]={0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
   static int awayTeamPasserAttempts[]={0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
   static int awayTeamPasserCompletions[]={0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
   static int awayTeamPasserYards[]={0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
   static int homeTeamPasser[]={0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
   static int homeTeamPasserAttempts[]={0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
   static int homeTeamPasserCompletions[]={0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
   static int homeTeamPasserYards[]={0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
   static int awayTeamRunRec[]={0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
   static int awayTeamRunRecAttempts[]={0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
   static int awayTeamRunRecYards[]={0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
   static int homeTeamRunRec[]={0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
   static int homeTeamRunRecAttempts[]={0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
   static int homeTeamRunRecYards[]={0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};

   public void init() throws ServletException
      {
      ServletContext context = getServletContext();
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
            throw new UnavailableException("Unable to create connection pool : " + error);
            }
          context.setAttribute("pool",pool);
          }
        }
      }

   public void doGet(HttpServletRequest request,
                     HttpServletResponse response)
                     throws UnavailableException,IOException
      {
      response.setContentType("text/html");
      response.setHeader("Refresh","3600");
      PrintWriter webPageOutput=response.getWriter();
      HttpSession session=request.getSession();
      Connection database=null;
      try
        {
        database=pool.getConnection(servletName);
        }
      catch(SQLException error)
        {
        throw new UnavailableException("Unable to connect to database : " + error);
        }
//      if(Routines.loginCheck(true,request,response,database,context))
//        {
//        return;
//        }
      int league=Routines.safeParseInt(request.getParameter("league"));
      boolean success      = true;
      int basePlayerNumber=league * 10000;
      try
        {
        Statement sql = database.createStatement();
        ResultSet queryResponse;
        // Clear all players Doctor & Trainer visits for the week.
        queryResponse=sql.executeQuery("UPDATE players " +
                                       "SET Doctors=0,Trainers=0,DateTimeStamp='" + Routines.getDateTime(false) + "' " +
                                       "WHERE League=" + league);
        // Get latest league info.
        queryResponse = sql.executeQuery("SELECT Season,Week,PreSeasonWeeks,RegularSeasonWeeks,PostSeasonWeeks " +
                                         "FROM   leagues " +
                                         "WHERE  LeagueNumber = " + league);
        queryResponse.first();
        int leagueSeason       = queryResponse.getInt(1);
        int leagueWeek         = queryResponse.getInt(2);
        int preSeasonWeeks     = queryResponse.getInt(3);
        int regularSeasonWeeks = queryResponse.getInt(4);
        int postSeasonWeeks    = queryResponse.getInt(5);
        int weeksPerSeason     = preSeasonWeeks + regularSeasonWeeks + postSeasonWeeks;
        int resultSeason       = 0;
        int resultWeek         = 0;
        // Calculate the next week details.
        if (leagueWeek<weeksPerSeason)
           {
           resultSeason = leagueSeason;
           resultWeek   = leagueWeek + 1;
           }
        else
           {
           resultSeason = leagueSeason + 1;
           resultWeek   = 1;
           }
        // Get first team in league.
        queryResponse = sql.executeQuery("SELECT TeamNumber " +
                                         "FROM   leagueteams,divisions,conferences " +
                                         "WHERE  leagueteams.DivisionNumber = divisions.DivisionNumber " +
                                         "AND    divisions.ConferenceNumber = conferences.ConferenceNumber " +
                                         "AND    conferences.LeagueNumber = " + league + " " +
                                         "ORDER  BY TeamNumber ASC");
        queryResponse.first();
        int baseTeamNumber = queryResponse.getInt(1) - 1;
        // Get latest playByPlayNumber.
        queryResponse = sql.executeQuery("SELECT PlayByPlayNumber " +
                                         "FROM   playbyplay " +
                                         "ORDER  BY PlayByPlayNumber DESC");
        int lastPlayByPlayNumber=0;
        if (queryResponse.first())
           {
           lastPlayByPlayNumber = queryResponse.getInt(1);
           }
        // Get last standingNumber.
        int standingNumber=0;
        queryResponse = sql.executeQuery("SELECT StandingNumber " +
                                         "FROM   standings " +
                                         "ORDER  BY StandingNumber DESC");
        if (queryResponse.first())
           {
           standingNumber = queryResponse.getInt(1);
           }
        // Get tempResults records.
        queryResponse = sql.executeQuery("SELECT Home,Away,Score_Home,Score_Away,Game,Results_ID,Attendance " +
                                         "FROM tempresults " +
                                         "WHERE Season = " + resultSeason + " " +
                                         "AND   Week   = " + resultWeek);
        Statement sql2 = database.createStatement();
        Statement sql3 = database.createStatement();
        ResultSet queryResponse2;
        ResultSet queryResponse3;
        // For each result...
        while(queryResponse.next())
           {
           int homeTeam      = queryResponse.getInt(1) + baseTeamNumber;
           int awayTeam      = queryResponse.getInt(2) + baseTeamNumber;
           int homeTeamScore = queryResponse.getInt(3);
           int awayTeamScore = queryResponse.getInt(4);
           int game          = queryResponse.getInt(5);
           int resultsID     = queryResponse.getInt(6);
           int attendance    = queryResponse.getInt(7);
           // Get fixtureNumber.
           int fixture=0;
           queryResponse2 = sql2.executeQuery("SELECT FixtureNumber " +
                                              "FROM   fixtures " +
                                              "WHERE  LeagueNumber = " +
                                              league + " " +
                                              "AND    Season = " + resultSeason + " " +
                                              "AND    Week   = " + resultWeek + " " +
                                              "AND    Game   = " + game + " " +
                                              "AND    HomeTeam = " + homeTeam + " " +
                                              "AND    AwayTeam = " + awayTeam);
           queryResponse2.first();
           fixture = queryResponse2.getInt(1);
           // Get tempPlayByPlayRecords.
           queryResponse2 = sql2.executeQuery("SELECT Clock,Offence,Result,Main," +
                                              "Rec,Result,Yards_Off,Yards_Def,ToGo," +
                                              "OffForm,DefForm,Play,Defender," +
                                              "OffPl1,OffPl2,OffPl3,OffPl4,OffPl5,OffPl6," +
                                              "OffPl7,OffPl8,OffPl9,OffPl10,OffPl11,DefPl1," +
                                              "DefPl2,DefPl3,DefPl4,DefPl5,DefPl6,DefPl7," +
                                              "DefPl8,DefPl9,DefPl10,DefPl11,Down,Distance " +
                                              "FROM   tempplaybyplay " +
                                              "WHERE  Game = " + resultsID + " " +
                                              "ORDER BY Game ASC, Match_ID ASC");
           int currentGame=0;
           int pbpAwayTeamScore=0;
           int pbpHomeTeamScore=0;
           int awayTeamScores[]={0,0,0,0,0};
           int homeTeamScores[]={0,0,0,0,0};
           int currentQuarter = 1;
           while(queryResponse2.next())
              {
              int clock              = queryResponse2.getInt(1);
              int offense            = queryResponse2.getInt(2);
              int playDescription    = queryResponse2.getInt(3);
              int primaryPlayer      = queryResponse2.getInt(4);
              if(primaryPlayer>0)
                {
                primaryPlayer+=basePlayerNumber;
                }
              int secondaryPlayer    = queryResponse2.getInt(5);
              if(secondaryPlayer>0)
                {
                secondaryPlayer+=basePlayerNumber;
                }
              int offensiveYards     = queryResponse2.getInt(7);
              int defensiveYards     = queryResponse2.getInt(8);
              int distanceToGoal     = queryResponse2.getInt(9);
              int offensiveFormation = Routines.safeParseInt(queryResponse2.getString(10));
              int defensiveFormation = Routines.safeParseInt(queryResponse2.getString(11));
              String play            = queryResponse2.getString(12);
              int defender           = queryResponse2.getInt(13);
              if(defender>0)
                {
                defender+=basePlayerNumber;
                }
              int offensivePlayer01  = queryResponse2.getInt(14);
              if(offensivePlayer01>0)
                {
                offensivePlayer01+=basePlayerNumber;
                }
              int offensivePlayer02  = queryResponse2.getInt(15);
              if(offensivePlayer02>0)
                {
                offensivePlayer02+=basePlayerNumber;
                }
              int offensivePlayer03  = queryResponse2.getInt(16);
              if(offensivePlayer03>0)
                {
                offensivePlayer03+=basePlayerNumber;
                }
              int offensivePlayer04  = queryResponse2.getInt(17);
              if(offensivePlayer04>0)
                {
                offensivePlayer04+=basePlayerNumber;
                }
              int offensivePlayer05  = queryResponse2.getInt(18);
              if(offensivePlayer05>0)
                {
                offensivePlayer05+=basePlayerNumber;
                }
              int offensivePlayer06  = queryResponse2.getInt(19);
              if(offensivePlayer06>0)
                {
                offensivePlayer06+=basePlayerNumber;
                }
              int offensivePlayer07  = queryResponse2.getInt(20);
              if(offensivePlayer07>0)
                {
                offensivePlayer07+=basePlayerNumber;
                }
              int offensivePlayer08  = queryResponse2.getInt(21);
              if(offensivePlayer08>0)
                {
                offensivePlayer08+=basePlayerNumber;
                }
              int offensivePlayer09  = queryResponse2.getInt(22);
              if(offensivePlayer09>0)
                {
                offensivePlayer09+=basePlayerNumber;
                }
              int offensivePlayer10  = queryResponse2.getInt(23);
              if(offensivePlayer10>0)
                {
                offensivePlayer10+=basePlayerNumber;
                }
              int offensivePlayer11  = queryResponse2.getInt(24);
              if(offensivePlayer11>0)
                {
                offensivePlayer11+=basePlayerNumber;
                }
              int defensivePlayer01  = queryResponse2.getInt(25);
              if(defensivePlayer01>0)
                {
                defensivePlayer01+=basePlayerNumber;
                }
              int defensivePlayer02  = queryResponse2.getInt(26);
              if(defensivePlayer02>0)
                {
                defensivePlayer02+=basePlayerNumber;
                }
              int defensivePlayer03  = queryResponse2.getInt(27);
              if(defensivePlayer03>0)
                {
                defensivePlayer03+=basePlayerNumber;
                }
              int defensivePlayer04  = queryResponse2.getInt(28);
              if(defensivePlayer04>0)
                {
                defensivePlayer04+=basePlayerNumber;
                }
              int defensivePlayer05  = queryResponse2.getInt(29);
              if(defensivePlayer05>0)
                {
                defensivePlayer05+=basePlayerNumber;
                }
              int defensivePlayer06  = queryResponse2.getInt(30);
              if(defensivePlayer06>0)
                {
                defensivePlayer06+=basePlayerNumber;
                }
              int defensivePlayer07  = queryResponse2.getInt(31);
              if(defensivePlayer07>0)
                {
                defensivePlayer07+=basePlayerNumber;
                }
              int defensivePlayer08  = queryResponse2.getInt(32);
              if(defensivePlayer08>0)
                {
                defensivePlayer08+=basePlayerNumber;
                }
              int defensivePlayer09  = queryResponse2.getInt(33);
              if(defensivePlayer09>0)
                {
                defensivePlayer09+=basePlayerNumber;
                }
              int defensivePlayer10  = queryResponse2.getInt(34);
              if(defensivePlayer10>0)
                {
                defensivePlayer10+=basePlayerNumber;
                }
              int defensivePlayer11  = queryResponse2.getInt(35);
              if(defensivePlayer11>0)
                {
                defensivePlayer11+=basePlayerNumber;
                }
              int down               = queryResponse2.getInt(36);
              int distance           = queryResponse2.getInt(37);
              // Put tempPlayByPlayRecords into PlayByPlay record.
              lastPlayByPlayNumber++;
              int tempHomeTeamInPossession=0;
              if (offense==0)
                 {
                 tempHomeTeamInPossession=1;
                 }
              queryResponse3 = sql3.executeQuery("INSERT INTO playbyplay (" +
                                                 "PlayByPlayNumber,Fixture,Clock," +
                                                 "HomeTeamInPossession,Down,Distance," +
                                                 "DistanceToGoal,OffensiveFormation," +
                                                 "DefensiveFormation,Play,InitialBallCarrier," +
                                                 "SecondaryBallCarrier,Defender,OffensiveYards," +
                                                 "DefensiveYards,Description," +
                                                 "OffensivePlayer01,OffensivePlayer02," +
                                                 "OffensivePlayer03,OffensivePlayer04," +
                                                 "OffensivePlayer05,OffensivePlayer06," +
                                                 "OffensivePlayer07,OffensivePlayer08," +
                                                 "OffensivePlayer09,OffensivePlayer10," +
                                                 "OffensivePlayer11,DefensivePlayer01," +
                                                 "DefensivePlayer02,DefensivePlayer03," +
                                                 "DefensivePlayer04,DefensivePlayer05," +
                                                 "DefensivePlayer06,DefensivePlayer07," +
                                                 "DefensivePlayer08,DefensivePlayer09," +
                                                 "DefensivePlayer10,DefensivePlayer11," +
                                                 "League,DateTimeStamp) " +
                                                 "VALUES (" +
                                                 lastPlayByPlayNumber + "," +
                                                 fixture + "," +
                                                 clock + "," +
                                                 tempHomeTeamInPossession + "," +
                                                 down + "," +
                                                 distance + "," +
                                                 distanceToGoal + "," +
                                                 offensiveFormation + "," +
                                                 defensiveFormation + "," +
                                                 "0" + "," +
                                                 primaryPlayer + "," +
                                                 secondaryPlayer + "," +
                                                 defender + "," +
                                                 offensiveYards + "," +
                                                 defensiveYards + "," +
                                                 playDescription + "," +
                                                 offensivePlayer01 + "," +
                                                 offensivePlayer02 + "," +
                                                 offensivePlayer03 + "," +
                                                 offensivePlayer04 + "," +
                                                 offensivePlayer05 + "," +
                                                 offensivePlayer06 + "," +
                                                 offensivePlayer07 + "," +
                                                 offensivePlayer08 + "," +
                                                 offensivePlayer09 + "," +
                                                 offensivePlayer10 + "," +
                                                 offensivePlayer11 + "," +
                                                 defensivePlayer01 + "," +
                                                 defensivePlayer02 + "," +
                                                 defensivePlayer03 + "," +
                                                 defensivePlayer04 + "," +
                                                 defensivePlayer05 + "," +
                                                 defensivePlayer06 + "," +
                                                 defensivePlayer07 + "," +
                                                 defensivePlayer08 + "," +
                                                 defensivePlayer09 + "," +
                                                 defensivePlayer10 + "," +
                                                 defensivePlayer11 + "," +
                                                 league + ",'" +
                                                 Routines.getDateTime(false) + "')");
              if (clock>(currentQuarter*900))
                 {
                 currentQuarter++;
                 }
              boolean homeTeamInPossession=false;
              if (offense==0)
                 {
                 homeTeamInPossession=true;
                 }
              else
                 {
                 homeTeamInPossession=false;
                 }
              // Pass Attempts.
              bestPlayersTableHandler(homeTeamInPossession,
                                      primaryPlayer,
                                      secondaryPlayer,
                                      playDescription,
                                      offensiveYards,
                                      defensiveYards);
              // Safety scored.
              if (playDescription==81)
                 {
                 if (homeTeamInPossession)
                    {
                    awayTeamScores[currentQuarter-1]+=2;
                    pbpAwayTeamScore+=2;
                    }
                 else
                    {
                    homeTeamScores[currentQuarter-1]+=2;
                    pbpHomeTeamScore+=2;
                    }
                 }
              // Field Goal scored.
              if (playDescription==56)
                 {
                 if (homeTeamInPossession)
                    {
                    homeTeamScores[currentQuarter-1]+=3;
                    pbpHomeTeamScore+=3;
                    }
                 else
                    {
                    awayTeamScores[currentQuarter-1]+=3;
                    pbpAwayTeamScore+=3;
                    }
                 }
              // Touchdown scored.
              if (playDescription>=61 && playDescription<=66)
                 {
                 if (homeTeamInPossession)
                    {
                    homeTeamScores[currentQuarter-1]+=6;
                    pbpHomeTeamScore+=6;
                    }
                 else
                    {
                    awayTeamScores[currentQuarter-1]+=6;
                    pbpAwayTeamScore+=6;
                    }
                 }
              // Touchdown return scored.
              if (playDescription>=67 && playDescription<=70)
                 {
                 if (homeTeamInPossession)
                    {
                    awayTeamScores[currentQuarter-1]+=6;
                    pbpAwayTeamScore+=6;
                    }
                 else
                    {
                    homeTeamScores[currentQuarter-1]+=6;
                    pbpHomeTeamScore+=6;
                    }
                 }
              // XP scored.
              if (playDescription==84)
                 {
                 if (homeTeamInPossession)
                    {
                    homeTeamScores[currentQuarter-1]+=1;
                    pbpHomeTeamScore+=1;
                    }
                 else
                    {
                    awayTeamScores[currentQuarter-1]+=1;
                    pbpAwayTeamScore+=1;
                    }
                 }
              // 2XP scored.
              if (playDescription==86)
                 {
                 if (homeTeamInPossession)
                    {
                    homeTeamScores[currentQuarter-1]+=2;
                    pbpHomeTeamScore+=2;
                    }
                 else
                    {
                    awayTeamScores[currentQuarter-1]+=2;
                    pbpAwayTeamScore+=2;
                    }
                 }
              }
           int homeTeamBestPasser            = 0;
           int homeTeamBestPasserAttempts    = 0;
           int homeTeamBestPasserCompletions = 0;
           int homeTeamBestPasserYards       = 0;
           int awayTeamBestPasser            = 0;
           int awayTeamBestPasserAttempts    = 0;
           int awayTeamBestPasserCompletions = 0;
           int awayTeamBestPasserYards       = 0;
           int homeTeamBestRunRec            = 0;
           int homeTeamBestRunRecAttempts    = 0;
           int homeTeamBestRunRecYards       = 0;
           int awayTeamBestRunRec            = 0;
           int awayTeamBestRunRecAttempts    = 0;
           int awayTeamBestRunRecYards       = 0;
           for(int currentPlayer=0;currentPlayer<homeTeamPasser.length;currentPlayer++)
              {
              if (homeTeamPasserYards[currentPlayer]>homeTeamBestPasserYards ||
                  homeTeamBestPasser == 0)
                 {
                 homeTeamBestPasser            = homeTeamPasser[currentPlayer];
                 homeTeamBestPasserAttempts    = homeTeamPasserAttempts[currentPlayer];
                 homeTeamBestPasserCompletions = homeTeamPasserCompletions[currentPlayer];
                 homeTeamBestPasserYards       = homeTeamPasserYards[currentPlayer];
                 }
              if (awayTeamPasserYards[currentPlayer]>awayTeamBestPasserYards ||
                  awayTeamBestPasser == 0)
                 {
                 awayTeamBestPasser            = awayTeamPasser[currentPlayer];
                 awayTeamBestPasserAttempts    = awayTeamPasserAttempts[currentPlayer];
                 awayTeamBestPasserCompletions = awayTeamPasserCompletions[currentPlayer];
                 awayTeamBestPasserYards       = awayTeamPasserYards[currentPlayer];
                 }
              if (homeTeamRunRecYards[currentPlayer]>homeTeamBestRunRecYards ||
                  homeTeamBestRunRec == 0)
                 {
                 homeTeamBestRunRec            = homeTeamRunRec[currentPlayer];
                 homeTeamBestRunRecAttempts    = homeTeamRunRecAttempts[currentPlayer];
                 homeTeamBestRunRecYards       = homeTeamRunRecYards[currentPlayer];
                 }
              if (awayTeamRunRecYards[currentPlayer]>awayTeamBestRunRecYards ||
                  awayTeamBestRunRec == 0)
                 {
                 awayTeamBestRunRec            = awayTeamRunRec[currentPlayer];
                 awayTeamBestRunRecAttempts    = awayTeamRunRecAttempts[currentPlayer];
                 awayTeamBestRunRecYards       = awayTeamRunRecYards[currentPlayer];
                 }
              }
           if (homeTeamBestPasser == 0)
              {
              homeTeamBestPasser = getStartingPlayer(true,homeTeam,database);
              }
           if (awayTeamBestPasser == 0)
              {
              awayTeamBestPasser = getStartingPlayer(true,awayTeam,database);
              }
           if (homeTeamBestRunRec == 0)
              {
              homeTeamBestRunRec = getStartingPlayer(false,homeTeam,database);
              }
           if (awayTeamBestRunRec == 0)
              {
              awayTeamBestRunRec = getStartingPlayer(false,awayTeam,database);
              }
           queryResponse2 = sql2.executeQuery("UPDATE fixtures " +
                                              "SET HomeTeamScore = " + homeTeamScore +
                                              ",AwayTeamScore = " + awayTeamScore +
                                              ",FinalQuarter = " + currentQuarter +
                                              ",HomeTeamScore1 = " + homeTeamScores[0] +
                                              ",HomeTeamScore2 = " + homeTeamScores[1] +
                                              ",HomeTeamScore3 = " + homeTeamScores[2] +
                                              ",HomeTeamScore4 = " + homeTeamScores[3] +
                                              ",HomeTeamScore5 = " + homeTeamScores[4] +
                                              ",AwayTeamScore1 = " + awayTeamScores[0] +
                                              ",AwayTeamScore2 = " + awayTeamScores[1] +
                                              ",AwayTeamScore3 = " + awayTeamScores[2] +
                                              ",AwayTeamScore4 = " + awayTeamScores[3] +
                                              ",AwayTeamScore5 = " + awayTeamScores[4] +
                                              ",HomeTeamBestPasser = " + homeTeamBestPasser +
                                              ",HomeTeamBestPasserAttempts = " + homeTeamBestPasserAttempts +
                                              ",HomeTeamBestPasserCompletions = " + homeTeamBestPasserCompletions +
                                              ",HomeTeamBestPasserYards = " + homeTeamBestPasserYards +
                                              ",AwayTeamBestPasser = " + awayTeamBestPasser +
                                              ",AwayTeamBestPasserAttempts = " + awayTeamBestPasserAttempts +
                                              ",AwayTeamBestPasserCompletions = " + awayTeamBestPasserCompletions +
                                              ",AwayTeamBestPasserYards = " + awayTeamBestPasserYards +
                                              ",HomeTeamBestRunRec = " + homeTeamBestRunRec +
                                              ",HomeTeamBestRunRecAttempts = " + homeTeamBestRunRecAttempts +
                                              ",HomeTeamBestRunRecYards = " + homeTeamBestRunRecYards +
                                              ",AwayTeamBestRunRec = " + awayTeamBestRunRec +
                                              ",AwayTeamBestRunRecAttempts = " + awayTeamBestRunRecAttempts +
                                              ",AwayTeamBestRunRecYards = " + awayTeamBestRunRecYards +
                                              ",Attendance=" + attendance +
                                              ",DateTimeStamp='" + Routines.getDateTime(false) + "'" +
                                              " " +
                                              "WHERE LeagueNumber = " + league + " " +
                                              "AND Season = " + resultSeason + " " +
                                              "AND Week = " + resultWeek + " " +
                                              "AND HomeTeam = " + homeTeam + " " +
                                              "AND AwayTeam = " + awayTeam + " " +
                                              "AND Game = " + game);
           int awayTeamWins=0;
           int awayTeamLosses=0;
           int awayTeamDraws=0;
           int awayTeamCoachNumber=0;
           int awayTeamPosition=0;
           int awayTeamScored=0;
           int awayTeamConceded=0;
           int awayTeamStreak=0;
           int awayTeamHomeWins=0;
           int awayTeamHomeLosses=0;
           int awayTeamHomeDraws=0;
           int awayTeamAwayWins=0;
           int awayTeamAwayLosses=0;
           int awayTeamAwayDraws=0;
           int awayTeamDivisionNumber=0;
           int awayTeamDivisionWins=0;
           int awayTeamDivisionLosses=0;
           int awayTeamDivisionDraws=0;
           int awayTeamConferenceWins=0;
           int awayTeamConferenceLosses=0;
           int awayTeamConferenceDraws=0;
           int awayTeamInterWins=0;
           int awayTeamInterLosses=0;
           int awayTeamInterDraws=0;
           int awayTeamConferenceNumber=0;
           int homeTeamWins=0;
           int homeTeamLosses=0;
           int homeTeamDraws=0;
           int homeTeamCoachNumber=0;
           int homeTeamPosition=0;
           int homeTeamScored=0;
           int homeTeamConceded=0;
           int homeTeamStreak=0;
           int homeTeamHomeWins=0;
           int homeTeamHomeLosses=0;
           int homeTeamHomeDraws=0;
           int homeTeamAwayWins=0;
           int homeTeamAwayLosses=0;
           int homeTeamAwayDraws=0;
           int homeTeamDivisionNumber=0;
           int homeTeamDivisionWins=0;
           int homeTeamDivisionLosses=0;
           int homeTeamDivisionDraws=0;
           int homeTeamConferenceWins=0;
           int homeTeamConferenceLosses=0;
           int homeTeamConferenceDraws=0;
           int homeTeamInterWins=0;
           int homeTeamInterLosses=0;
           int homeTeamInterDraws=0;
           int homeTeamConferenceNumber=0;
           if(resultWeek==(Routines.safeParseInt((String)session.getAttribute("preSeasonWeeks")) + 1))
             {
             queryResponse2 = sql2.executeQuery("SELECT CoachNumber " +
                                                "FROM standings,leagueteams,divisions,conferences,leagues " +
                                                "WHERE standings.TeamNumber       = " + awayTeam + " AND " +
                                                "standings.TeamNumber             = leagueteams.TeamNumber       AND " +
                                                "leagueteams.DivisionNumber       = divisions.DivisionNumber     AND " +
                                                "divisions.ConferenceNumber       = conferences.ConferenceNumber AND " +
                                                "conferences.LeagueNumber         = leagues.LeagueNumber         AND " +
                                                "standings.Season                 = leagues.Season               AND " +
                                                "standings.Week                   = leagues.Week                     ");
             queryResponse2.first();
             awayTeamCoachNumber = queryResponse2.getInt(1);
             queryResponse2 = sql2.executeQuery("SELECT CoachNumber " +
                                                "FROM standings,leagueteams,divisions,conferences,leagues " +
                                                "WHERE standings.TeamNumber       = " + homeTeam + " AND " +
                                                "standings.TeamNumber             = leagueteams.TeamNumber       AND " +
                                                "leagueteams.DivisionNumber       = divisions.DivisionNumber     AND " +
                                                "divisions.ConferenceNumber       = conferences.ConferenceNumber AND " +
                                                "conferences.LeagueNumber         = leagues.LeagueNumber         AND " +
                                                "standings.Season                 = leagues.Season               AND " +
                                                "standings.Week                   = leagues.Week                     ");
             queryResponse2.first();
             homeTeamCoachNumber = queryResponse2.getInt(1);
             }
           else
             {
             queryResponse2 = sql2.executeQuery("SELECT Wins,Losses,Draws,CoachNumber," +
                                                "Position,Scored,Conceded,Streak," +
                                                "HomeWins,HomeLosses,HomeDraws," +
                                                "AwayWins,AwayLosses,AwayDraws," +
                                                "divisions.DivisionNumber,DivisionWins,DivisionLosses,DivisionDraws," +
                                                "ConferenceWins,ConferenceLosses,ConferenceDraws," +
                                                "InterWins,InterLosses,InterDraws, " +
                                                "divisions.ConferenceNumber " +
                                                "FROM standings,leagueteams,divisions,conferences,leagues " +
                                                "WHERE standings.TeamNumber       = " + awayTeam + " AND " +
                                                "standings.TeamNumber             = leagueteams.TeamNumber       AND " +
                                                "leagueteams.DivisionNumber       = divisions.DivisionNumber     AND " +
                                                "divisions.ConferenceNumber       = conferences.ConferenceNumber AND " +
                                                "conferences.LeagueNumber         = leagues.LeagueNumber         AND " +
                                                "standings.Season                 = leagues.Season               AND " +
                                                "standings.Week                   = leagues.Week                     ");
             queryResponse2.first();
             awayTeamWins             = queryResponse2.getInt(1);
             awayTeamLosses           = queryResponse2.getInt(2);
             awayTeamDraws            = queryResponse2.getInt(3);
             awayTeamCoachNumber      = queryResponse2.getInt(4);
             awayTeamPosition         = queryResponse2.getInt(5);
             awayTeamScored           = queryResponse2.getInt(6);
             awayTeamConceded         = queryResponse2.getInt(7);
             awayTeamStreak           = queryResponse2.getInt(8);
             awayTeamHomeWins         = queryResponse2.getInt(9);
             awayTeamHomeLosses       = queryResponse2.getInt(10);
             awayTeamHomeDraws        = queryResponse2.getInt(11);
             awayTeamAwayWins         = queryResponse2.getInt(12);
             awayTeamAwayLosses       = queryResponse2.getInt(13);
             awayTeamAwayDraws        = queryResponse2.getInt(14);
             awayTeamDivisionNumber   = queryResponse2.getInt(15);
             awayTeamDivisionWins     = queryResponse2.getInt(16);
             awayTeamDivisionLosses   = queryResponse2.getInt(17);
             awayTeamDivisionDraws    = queryResponse2.getInt(18);
             awayTeamConferenceWins   = queryResponse2.getInt(19);
             awayTeamConferenceLosses = queryResponse2.getInt(20);
             awayTeamConferenceDraws  = queryResponse2.getInt(21);
             awayTeamInterWins        = queryResponse2.getInt(22);
             awayTeamInterLosses      = queryResponse2.getInt(23);
             awayTeamInterDraws       = queryResponse2.getInt(24);
             awayTeamConferenceNumber = queryResponse2.getInt(25);
             queryResponse2 = sql2.executeQuery("SELECT Wins,Losses,Draws,CoachNumber," +
                                                "Position,Scored,Conceded,Streak," +
                                                "HomeWins,HomeLosses,HomeDraws," +
                                                "AwayWins,AwayLosses,AwayDraws," +
                                                "divisions.DivisionNumber,DivisionWins,DivisionLosses,DivisionDraws," +
                                                "ConferenceWins,ConferenceLosses,ConferenceDraws," +
                                                "InterWins,InterLosses,InterDraws, " +
                                                "divisions.ConferenceNumber " +
                                                "FROM standings,leagueteams,divisions,conferences,leagues " +
                                                "WHERE standings.TeamNumber       = " + homeTeam + " AND " +
                                                "standings.TeamNumber             = leagueteams.TeamNumber       AND " +
                                                "leagueteams.DivisionNumber       = divisions.DivisionNumber     AND " +
                                                "divisions.ConferenceNumber       = conferences.ConferenceNumber AND " +
                                                "conferences.LeagueNumber         = leagues.LeagueNumber         AND " +
                                                "standings.Season                 = leagues.Season               AND " +
                                                "standings.Week                   = leagues.Week                     ");
             queryResponse2.first();
             homeTeamWins             = queryResponse2.getInt(1);
             homeTeamLosses           = queryResponse2.getInt(2);
             homeTeamDraws            = queryResponse2.getInt(3);
             homeTeamCoachNumber      = queryResponse2.getInt(4);
             homeTeamPosition         = queryResponse2.getInt(5);
             homeTeamScored           = queryResponse2.getInt(6);
             homeTeamConceded         = queryResponse2.getInt(7);
             homeTeamStreak           = queryResponse2.getInt(8);
             homeTeamHomeWins         = queryResponse2.getInt(9);
             homeTeamHomeLosses       = queryResponse2.getInt(10);
             homeTeamHomeDraws        = queryResponse2.getInt(11);
             homeTeamAwayWins         = queryResponse2.getInt(12);
             homeTeamAwayLosses       = queryResponse2.getInt(13);
             homeTeamAwayDraws        = queryResponse2.getInt(14);
             homeTeamDivisionNumber   = queryResponse2.getInt(15);
             homeTeamDivisionWins     = queryResponse2.getInt(16);
             homeTeamDivisionLosses   = queryResponse2.getInt(17);
             homeTeamDivisionDraws    = queryResponse2.getInt(18);
             homeTeamConferenceWins   = queryResponse2.getInt(19);
             homeTeamConferenceLosses = queryResponse2.getInt(20);
             homeTeamConferenceDraws  = queryResponse2.getInt(21);
             homeTeamInterWins        = queryResponse2.getInt(22);
             homeTeamInterLosses      = queryResponse2.getInt(23);
             homeTeamInterDraws       = queryResponse2.getInt(24);
             homeTeamConferenceNumber = queryResponse2.getInt(25);
             }
           for (int currentTeam=0;currentTeam<2;currentTeam++)
              {
              int teamNumber=0;
              int scored=0;
              int conceded=0;
              int wins=0;
              int homeWins=0;
              int streak=0;
              int losses=0;
              int homeLosses=0;
              int draws=0;
              int homeDraws=0;
              int coachNumber=0;
              int position=0;
              int awayWins=0;
              int awayLosses=0;
              int awayDraws=0;
              int divisionNumber=0;
              int divisionWins=0;
              int divisionLosses=0;
              int divisionDraws=0;
              int conferenceWins=0;
              int conferenceDraws=0;
              int conferenceLosses=0;
              int interWins=0;
              int interLosses=0;
              int interDraws=0;
              boolean home;

              if (currentTeam==0)
                 {
                 teamNumber = awayTeam;
                 home       = false;
                 }
              else
                 {
                 teamNumber = homeTeam;
                 home       = true;
                 }

              if (home)
                 {
                 scored           = homeTeamScored + homeTeamScore;
                 conceded         = homeTeamConceded + awayTeamScore;
                 wins             = homeTeamWins;
                 homeWins         = homeTeamHomeWins;
                 streak           = homeTeamStreak;
                 losses           = homeTeamLosses;
                 homeLosses       = homeTeamHomeLosses;
                 draws            = homeTeamDraws;
                 homeDraws        = homeTeamHomeDraws;
                 coachNumber      = homeTeamCoachNumber;
                 position         = homeTeamPosition;
                 awayWins         = homeTeamAwayWins;
                 awayLosses       = homeTeamAwayLosses;
                 awayDraws        = homeTeamAwayDraws;
                 divisionNumber   = homeTeamDivisionNumber;
                 divisionWins     = homeTeamDivisionWins;
                 divisionLosses   = homeTeamDivisionLosses;
                 divisionDraws    = homeTeamDivisionDraws;
                 conferenceWins   = homeTeamConferenceWins;
                 conferenceLosses = homeTeamConferenceLosses;
                 conferenceDraws  = homeTeamConferenceDraws;
                 interWins        = homeTeamInterWins;
                 interLosses      = homeTeamInterLosses;
                 interDraws       = homeTeamInterDraws;
                 if (homeTeamScore>awayTeamScore)
                    {
                    wins++;
                    homeWins++;
                    if (streak>=0)
                       {
                       streak++;
                       }
                    else
                       {
                       streak=1;
                       }
                    if(homeTeamDivisionNumber==awayTeamDivisionNumber)
                       {
                       divisionWins++;
                       }
                    if(homeTeamConferenceNumber==awayTeamConferenceNumber)
                       {
                       conferenceWins++;
                       }
                    else
                       {
                       interWins++;
                       }
                    }
                 else
                    {
                    if (homeTeamScore<awayTeamScore)
                       {
                       losses++;
                       homeLosses++;
                       if (streak>=0)
                          {
                          streak=-1;
                          }
                       else
                          {
                          streak--;
                          }
                       if(homeTeamDivisionNumber==awayTeamDivisionNumber)
                          {
                          divisionLosses++;
                          }
                       if(homeTeamConferenceNumber==awayTeamConferenceNumber)
                          {
                          conferenceLosses++;
                          }
                       else
                          {
                          interLosses++;
                          }
                       }
                    else
                       {
                       if (homeTeamScore==awayTeamScore)
                          {
                          draws++;
                          homeDraws++;
                          streak=0;
                          if(homeTeamDivisionNumber==awayTeamDivisionNumber)
                             {
                             divisionDraws++;
                             }
                          if(homeTeamConferenceNumber==awayTeamConferenceNumber)
                             {
                             conferenceDraws++;
                             }
                          else
                             {
                             interDraws++;
                             }
                          }
                       }
                    }
                 }
              else
                 {
                 scored           = awayTeamScored + awayTeamScore;
                 conceded         = awayTeamConceded + homeTeamScore;
                 wins             = awayTeamWins;
                 homeWins         = awayTeamHomeWins;
                 streak           = awayTeamStreak;
                 losses           = awayTeamLosses;
                 homeLosses       = awayTeamHomeLosses;
                 draws            = awayTeamDraws;
                 homeDraws        = awayTeamHomeDraws;
                 coachNumber      = awayTeamCoachNumber;
                 position         = awayTeamPosition;
                 awayWins         = awayTeamAwayWins;
                 awayLosses       = awayTeamAwayLosses;
                 awayDraws        = awayTeamAwayDraws;
                 divisionNumber   = awayTeamDivisionNumber;
                 divisionWins     = awayTeamDivisionWins;
                 divisionLosses   = awayTeamDivisionLosses;
                 divisionDraws    = awayTeamDivisionDraws;
                 conferenceWins   = awayTeamConferenceWins;
                 conferenceLosses = awayTeamConferenceLosses;
                 conferenceDraws  = awayTeamConferenceDraws;
                 interWins        = awayTeamInterWins;
                 interLosses      = awayTeamInterLosses;
                 interDraws       = awayTeamInterDraws;
                 if (awayTeamScore>homeTeamScore)
                    {
                    wins++;
                    awayWins++;
                    if (streak>=0)
                       {
                       streak++;
                       }
                    else
                       {
                       streak=1;
                       }
                    if(homeTeamDivisionNumber==awayTeamDivisionNumber)
                       {
                       divisionWins++;
                       }
                    if(homeTeamConferenceNumber==awayTeamConferenceNumber)
                       {
                       conferenceWins++;
                       }
                    else
                       {
                       interWins++;
                       }
                    }
                 else
                    {
                    if (awayTeamScore<homeTeamScore)
                       {
                       losses++;
                       awayLosses++;
                       if (streak>=0)
                          {
                          streak=-1;
                          }
                       else
                          {
                          streak--;
                          }
                       if(homeTeamDivisionNumber==awayTeamDivisionNumber)
                          {
                          divisionLosses++;
                          }
                       if(homeTeamConferenceNumber==awayTeamConferenceNumber)
                          {
                          conferenceLosses++;
                          }
                       else
                          {
                          interLosses++;
                          }
                       }
                    else
                       {
                       if (homeTeamScore==awayTeamScore)
                          {
                          draws++;
                          awayDraws++;
                          streak=0;
                          if(homeTeamDivisionNumber==awayTeamDivisionNumber)
                             {
                             divisionDraws++;
                             }
                          if(homeTeamConferenceNumber==awayTeamConferenceNumber)
                             {
                             conferenceDraws++;
                             }
                          else
                             {
                             interDraws++;
                             }
                          }
                       }
                    }
                 }
              standingNumber++;
              queryResponse2 = sql2.executeQuery("INSERT INTO standings " +
                                                 "(StandingNumber,LeagueNumber,Season,Week,TeamNumber," +
                                                 "CoachNumber,Position," +
                                                 "Wins,Losses,Draws," +
                                                 "Scored,Conceded,Streak," +
                                                 "DivisionWins,DivisionLosses,DivisionDraws," +
                                                 "ConferenceWins,ConferenceLosses,ConferenceDraws," +
                                                 "InterWins,InterLosses,InterDraws," +
                                                 "HomeWins,HomeLosses,HomeDraws," +
                                                 "AwayWins,AwayLosses,AwayDraws,DivisionNumber,DateTimeStamp) " +
                                                 "VALUES (" +
                                                 standingNumber + "," +
                                                 league + "," +
                                                 resultSeason + "," +
                                                 resultWeek + "," +
                                                 teamNumber + "," +
                                                 coachNumber + "," +
                                                 position + "," +
                                                 wins + "," +
                                                 losses + "," +
                                                 draws + "," +
                                                 scored + "," +
                                                 conceded + "," +
                                                 streak + "," +
                                                 divisionWins + "," +
                                                 divisionLosses + "," +
                                                 divisionDraws + "," +
                                                 conferenceWins + "," +
                                                 conferenceLosses + "," +
                                                 conferenceDraws + "," +
                                                 interWins + "," +
                                                 interLosses + "," +
                                                 interDraws + "," +
                                                 homeWins + "," +
                                                 homeLosses + "," +
                                                 homeDraws + "," +
                                                 awayWins + "," +
                                                 awayLosses + "," +
                                                 awayDraws + "," +
                                                 divisionNumber + ",'" +
                                                 Routines.getDateTime(false) + "')");
              }
           resetTableContents();
           }
        queryResponse = sql.executeQuery("SELECT TeamNumber,(Scored - Conceded) " +
                                         "FROM   standings " +
                                         "WHERE  standings.LeagueNumber = " + league + " " +
                                         "AND    standings.Season       = " + resultSeason + " " +
                                         "AND    standings.Week         = " + resultWeek + " " +
                                         "ORDER BY Wins DESC,Draws DESC," +
                                         "DivisionWins DESC,DivisionDraws DESC," +
                                         "ConferenceWins DESC,ConferenceDraws DESC," +
                                         "InterWins DESC,InterDraws DESC," +
                                         "2 DESC,Scored DESC,Conceded ASC");
        int newPosition = 0;
        while(queryResponse.next())
           {
           newPosition++;
           queryResponse2 = sql2.executeQuery("UPDATE standings " +
                                              "SET Position = " + newPosition + "," +
                                              "DateTimeStamp='" + Routines.getDateTime(false) + "' " +
                                              "WHERE  standings.LeagueNumber = " + league + " " +
                                              "AND    standings.Season       = " + resultSeason + " " +
                                              "AND    standings.Week         = " + resultWeek + " " +
                                              "AND    TeamNumber             = " + queryResponse.getInt(1));
           }
        queryResponse = sql.executeQuery("UPDATE leagues " +
                                         "SET Season = " + resultSeason +
                                         ",Week = " + resultWeek + "," +
                                         "DateTimeStamp='" + Routines.getDateTime(false) + "' " +
                                         "WHERE LeagueNumber = " + league);
        queryResponse = sql.executeQuery("SELECT Results_ID " +
                                         "FROM   tempresults " +
                                         "WHERE  Season = " + resultSeason + " " +
                                         "AND    Week   = " + resultWeek);
        while(queryResponse.next())
           {
           queryResponse2 = sql2.executeQuery("DELETE FROM tempplaybyplay " +
                                              "WHERE Game = " + queryResponse.getInt(1));
           }
        queryResponse = sql.executeQuery("DELETE FROM tempresults " +
                                         "WHERE Season = " + resultSeason + " " +
                                         "AND   Week   = " + resultWeek);
        }
      catch(Exception error)
        {
        success = false;
        session.setAttribute("message",error.getMessage());
        }
      pool.returnConnection(database);
      if(success)
         {
         session.setAttribute("message","Results processed without error.");
         }
//      response.sendRedirect(Routines.getRedirect(request,response,context));
      return;
   }

   static void bestPlayersTableHandler(boolean homeTeamInPossession,
                                       int primaryPlayer,
                                       int secondaryPlayer,
                                       int playDescription,
                                       int offensiveYards,
                                       int defensiveYards)
     {
     // Passing Play
     if ((playDescription>=30 && playDescription<=49)||
         (playDescription>=64 && playDescription<=67)||
         (playDescription>=74 && playDescription<=79)||
         (playDescription>=88 && playDescription<=97))
        {
        boolean tableUpdated=false;
        if (homeTeamInPossession)
           {
           for(int currentPlayer=0;currentPlayer<homeTeamPasser.length;currentPlayer++)
              {
              if(!tableUpdated && homeTeamPasser[currentPlayer]==0)
                 {
                 homeTeamPasser[currentPlayer]=primaryPlayer;
                 }
              if(homeTeamPasser[currentPlayer]==primaryPlayer)
                 {
                 allocateStats(true,
                               currentPlayer,
                               homeTeamInPossession,
                               primaryPlayer,
                               secondaryPlayer,
                               playDescription,
                               offensiveYards,
                               defensiveYards);
                 tableUpdated=true;
                 }
              }
           }
        else
           {
           for(int currentPlayer=0;currentPlayer<awayTeamPasser.length;currentPlayer++)
              {
              if(!tableUpdated && awayTeamPasser[currentPlayer]==0)
                 {
                 awayTeamPasser[currentPlayer]=primaryPlayer;
                 }
              if(awayTeamPasser[currentPlayer]==primaryPlayer)
                 {
                 allocateStats(true,
                               currentPlayer,
                               homeTeamInPossession,
                               primaryPlayer,
                               secondaryPlayer,
                               playDescription,
                               offensiveYards,
                               defensiveYards);
                 tableUpdated=true;
                 }
              }
           }
           if(!tableUpdated)
              {
              System.out.println("*** Best passing player table full, increase size ***");
              }
        }
     // Receiving play.
     if ((playDescription>=40 && playDescription<=49)||
         (playDescription>=64 && playDescription<=66))
        {
        boolean tableUpdated=false;
        if (homeTeamInPossession)
           {
           for(int currentPlayer=0;currentPlayer<homeTeamRunRec.length;currentPlayer++)
              {
              if(!tableUpdated && homeTeamRunRec[currentPlayer]==0)
                 {
                 homeTeamRunRec[currentPlayer]=secondaryPlayer;
                 }
              if(homeTeamRunRec[currentPlayer]==secondaryPlayer)
                 {
                 allocateStats(false,
                               currentPlayer,
                               homeTeamInPossession,
                               primaryPlayer,
                               secondaryPlayer,
                               playDescription,
                               offensiveYards,
                               defensiveYards);
                 tableUpdated=true;
                 }
              }
           }
        else
           {
           for(int currentPlayer=0;currentPlayer<awayTeamRunRec.length;currentPlayer++)
              {
              if(!tableUpdated && awayTeamRunRec[currentPlayer]==0)
                 {
                 awayTeamRunRec[currentPlayer]=secondaryPlayer;
                 }
              if(awayTeamRunRec[currentPlayer]==secondaryPlayer)
                 {
                 allocateStats(false,
                               currentPlayer,
                               homeTeamInPossession,
                               primaryPlayer,
                               secondaryPlayer,
                               playDescription,
                               offensiveYards,
                               defensiveYards);
                 tableUpdated=true;
                 }
              }
           }
           if(!tableUpdated)
              {
              System.out.println("*** Best Rec player table full, increase size ***");
              }
        }
     // Receiving play.
     if ((playDescription>=10 && playDescription<=29)||
         (playDescription>=61 && playDescription<=63))
        {
        boolean tableUpdated=false;
        if (homeTeamInPossession)
           {
           for(int currentPlayer=0;currentPlayer<homeTeamRunRec.length;currentPlayer++)
              {
              if(!tableUpdated && homeTeamRunRec[currentPlayer]==0)
                 {
                 homeTeamRunRec[currentPlayer]=primaryPlayer;
                 }
              if(homeTeamRunRec[currentPlayer]==primaryPlayer)
                 {
                 allocateStats(false,
                               currentPlayer,
                               homeTeamInPossession,
                               primaryPlayer,
                               secondaryPlayer,
                               playDescription,
                               offensiveYards,
                               defensiveYards);
                 tableUpdated=true;
                 }
              }
           }
        else
           {
           for(int currentPlayer=0;currentPlayer<awayTeamRunRec.length;currentPlayer++)
              {
              if(!tableUpdated && awayTeamRunRec[currentPlayer]==0)
                 {
                 awayTeamRunRec[currentPlayer]=primaryPlayer;
                 }
              if(awayTeamRunRec[currentPlayer]==primaryPlayer)
                 {
                 allocateStats(false,
                               currentPlayer,
                               homeTeamInPossession,
                               primaryPlayer,
                               secondaryPlayer,
                               playDescription,
                               offensiveYards,
                               defensiveYards);
                 tableUpdated=true;
                 }
              }
           }
           if(!tableUpdated)
              {
              System.out.println("*** Best Run player table full, increase size ***");
              }
        }
     }

  static void allocateStats(boolean passer,
                            int     currentPlayer,
                            boolean homeTeamInPossession,
                            int     primaryPlayer,
                            int     secondaryPlayer,
                            int     playDescription,
                            int     offensiveYards,
                            int     defensiveYards)
     {
     // Sack
     if (playDescription==80)
        {
        if (homeTeamInPossession)
           {
           homeTeamPasserYards[currentPlayer]+=offensiveYards;
           }
        else
           {
           awayTeamPasserYards[currentPlayer]=+offensiveYards;
           }
        }
     // Pass Incomplete or Intercepted
     if ((playDescription>=30 && playDescription<=39)||
         (playDescription>=74 && playDescription<=79)||
         (playDescription>=88 && playDescription<=97)||
          playDescription==67)
        {
        if (homeTeamInPossession)
           {
           homeTeamPasserAttempts[currentPlayer]++;
           }
        else
           {
           awayTeamPasserAttempts[currentPlayer]++;
           }
        }
     // Pass Complete
     if (passer && (
                   (playDescription>=64 && playDescription<=66)||
                   (playDescription>=40 && playDescription<=49)
                   ))
        {
        if (homeTeamInPossession)
           {
           homeTeamPasserAttempts[currentPlayer]++;
           homeTeamPasserCompletions[currentPlayer]++;
           homeTeamPasserYards[currentPlayer]+=offensiveYards;
           }
        else
           {
           awayTeamPasserAttempts[currentPlayer]++;
           awayTeamPasserCompletions[currentPlayer]++;
           awayTeamPasserYards[currentPlayer]+=offensiveYards;
           }
        }
     // Run
     if (!passer && (
                    (playDescription>=61 && playDescription<=63)||
                    (playDescription>=10 && playDescription<=29)||
                    (playDescription>=64 && playDescription<=66)||
                    (playDescription>=40 && playDescription<=49)
                    ))
        {
        if (homeTeamInPossession)
           {
           homeTeamRunRecAttempts[currentPlayer]++;
           homeTeamRunRecYards[currentPlayer]+=offensiveYards;
           }
        else
           {
           awayTeamRunRecAttempts[currentPlayer]++;
           awayTeamRunRecYards[currentPlayer]+=offensiveYards;
           }
        }
     }

   static void resetTableContents()
      {
      for(int currentPlayer=0;currentPlayer<homeTeamPasser.length;currentPlayer++)
         {
         homeTeamPasser[currentPlayer]=0;
         homeTeamPasserAttempts[currentPlayer]=0;
         homeTeamPasserCompletions[currentPlayer]=0;
         homeTeamPasserYards[currentPlayer]=0;
         awayTeamPasser[currentPlayer]=0;
         awayTeamPasserAttempts[currentPlayer]=0;
         awayTeamPasserCompletions[currentPlayer]=0;
         awayTeamPasserYards[currentPlayer]=0;
         homeTeamRunRec[currentPlayer]=0;
         homeTeamRunRecAttempts[currentPlayer]=0;
         homeTeamRunRecYards[currentPlayer]=0;
         awayTeamRunRec[currentPlayer]=0;
         awayTeamRunRecAttempts[currentPlayer]=0;
         awayTeamRunRecYards[currentPlayer]=0;
         }
      }

   static int getStartingPlayer(boolean passer,
                                int team,
                                Connection database)
          throws UnavailableException
      {
      int player = 0;
      String position;
      if (passer)
         {
         position = "QB";
         }
      else
         {
         position = "WR";
         }
      try
         {
         Statement sql = database.createStatement();
         ResultSet queryResponse;
         queryResponse = sql.executeQuery("SELECT pl1 " +
                                          "FROM depthchart " +
                                          "WHERE Team = " + team + " " +
                                          "AND Position = '" + position + "'");
         if(queryResponse.first())
           {
           player = queryResponse.getInt(1);
           }
         }
      catch(SQLException error)
        {
        throw new UnavailableException("Database error getting default player : " + error);
        }
      return player;
      }

   static void playOffs(int league,
                        int season,
                        int week,
                        Connection database,
	                    ServletContext context)
      {
      System.out.println("Started");
      int numOfConferences=2;
      int numOfTeams=100;
      int[][] currentTeams=new int[numOfConferences][numOfTeams];
      int[][] currentWins=new int[numOfConferences][numOfTeams];
      int[][] currentDraws=new int[numOfConferences][numOfTeams];
      int[][] currentLosses=new int[numOfConferences][numOfTeams];
      int[][] currentDivisions=new int[numOfConferences][numOfTeams];
      int[][] projectedTeams=new int[numOfConferences][numOfTeams];
      int[][] projectedWins=new int[numOfConferences][numOfTeams];
      int[][] projectedDraws=new int[numOfConferences][numOfTeams];
      int[][] projectedLosses=new int[numOfConferences][numOfTeams];
      int currentConferenceNumber=0;
      int currentConference=-1;
      int maxNumOfConferences=-1;
      int maxNumOfTeams=-1;
      int weeksRemaining=0;
      int tempTeam=0;
      int tempWins=0;
      int tempDraws=0;
      int tempLosses=0;
      try
         {
         Statement sql = database.createStatement();
         ResultSet queryResponse;
         queryResponse = sql.executeQuery("SELECT TeamNumber,Wins,Draws,Losses,conferences.ConferenceNumber,standings.DivisionNumber " +
                                          "FROM standings,divisions,conferences " +
                                          "WHERE standings.LeagueNumber = " + league + " " +
                                          "AND Season = " + season + " " +
                                          "AND Week = " + week + " " +
                                          "AND standings.DivisionNumber=divisions.DivisionNumber " +
                                          "AND divisions.ConferenceNumber=conferences.ConferenceNumber " +
                                          "ORDER BY conferences.ConferenceNumber ASC, Wins DESC,Draws DESC,Losses ASC");
         while(queryResponse.next())
           {
           System.out.println("Found Team");
           if(currentConferenceNumber!=queryResponse.getInt(5))
             {
             currentConference++;
             maxNumOfConferences++;
             maxNumOfTeams=-1;
             currentConferenceNumber=queryResponse.getInt(5);
             System.out.println("Conference=" + currentConference);
             }
           maxNumOfTeams++;
           currentTeams[currentConference][maxNumOfTeams]=queryResponse.getInt(1);
           currentWins[currentConference][maxNumOfTeams]=queryResponse.getInt(2);
           currentDraws[currentConference][maxNumOfTeams]=queryResponse.getInt(3);
           currentLosses[currentConference][maxNumOfTeams]=queryResponse.getInt(4);
           currentDivisions[currentConference][maxNumOfTeams]=queryResponse.getInt(6);
           }
         }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Database getting current standings : " + error,false,context);	
        }
      try
         {
         Statement sql = database.createStatement();
         ResultSet queryResponse;
         queryResponse = sql.executeQuery("SELECT RegularSeasonWeeks " +
                                          "FROM leagues " +
                                          "WHERE LeagueNumber = " + league);
         if(queryResponse.first())
           {
           weeksRemaining=queryResponse.getInt(1)-week;
           }
         else
           {
		   Routines.writeToLog(servletName,"League not found (LeagueNumber=" + league + ")",false,context);	
           }
         }
      catch(SQLException error)
        {
		Routines.writeToLog(servletName,"Database getting league : " + error,false,context);	
        }
      System.out.println("WeeksRemaining=" + weeksRemaining);
      for(currentConference=0;currentConference<numOfConferences;currentConference++)
         {
         for(int currentTeam=0;currentTeam<maxNumOfTeams;currentTeam++)
            {
            projectedTeams[currentConference][currentTeam]=currentTeams[currentConference][currentTeam];
            projectedDraws[currentConference][currentTeam]=currentDraws[currentConference][currentTeam];
            if(currentTeam<6)
              {
              projectedWins[currentConference][currentTeam]=currentWins[currentConference][currentTeam];
              projectedLosses[currentConference][currentTeam]=currentLosses[currentConference][currentTeam]+weeksRemaining;
              }
            else
              {
              projectedWins[currentConference][currentTeam]=currentWins[currentConference][currentTeam]+weeksRemaining;
              projectedLosses[currentConference][currentTeam]=currentLosses[currentConference][currentTeam];
              }
            }
         }
      System.out.println("Sorting");
      for(currentConference=0;currentConference<numOfConferences;currentConference++)
         {
         boolean sorted=false;
         while(!sorted)
              {
              sorted=true;
              for(int currentTeam=0;currentTeam<maxNumOfTeams;currentTeam++)
                 {
                 if(projectedWins[currentConference][currentTeam]<projectedWins[currentConference][currentTeam+1]||
                   (projectedWins[currentConference][currentTeam]==projectedWins[currentConference][currentTeam+1]&&
                    projectedDraws[currentConference][currentTeam]<projectedDraws[currentConference][currentTeam+1]))
                   {
                   sorted=false;
                   tempTeam=projectedTeams[currentConference][currentTeam];
                   tempWins=projectedWins[currentConference][currentTeam];
                   tempDraws=projectedDraws[currentConference][currentTeam];
                   tempLosses=projectedLosses[currentConference][currentTeam];
                   projectedTeams[currentConference][currentTeam]=projectedTeams[currentConference][currentTeam+1];
                   projectedWins[currentConference][currentTeam]=projectedWins[currentConference][currentTeam+1];
                   projectedDraws[currentConference][currentTeam]=projectedDraws[currentConference][currentTeam+1];
                   projectedLosses[currentConference][currentTeam]=projectedLosses[currentConference][currentTeam+1];
                   projectedTeams[currentConference][currentTeam+1]=tempTeam;
                   projectedWins[currentConference][currentTeam+1]=tempWins;
                   projectedDraws[currentConference][currentTeam+1]=tempDraws;
                   projectedLosses[currentConference][currentTeam+1]=tempLosses;
                   }
                 }
              }
         }
      System.out.println("maxNumOfTeams=" + maxNumOfTeams);
      for(currentConference=0;currentConference<numOfConferences;currentConference++)
         {
         System.out.println(" ");
         for(int currentTeam=0;currentTeam<maxNumOfTeams;currentTeam++)
            {
            System.out.println(currentTeams[currentConference][currentTeam] + "-" +
                               currentWins[currentConference][currentTeam] + "-" +
                               currentLosses[currentConference][currentTeam] + "-" +
                               currentDraws[currentConference][currentTeam] + " " +
                               projectedTeams[currentConference][currentTeam] + "-" +
                               projectedWins[currentConference][currentTeam] + "-" +
                               projectedLosses[currentConference][currentTeam] + "-" +
                               projectedDraws[currentConference][currentTeam] + " ");
            }
         }
      System.out.println("Finished");
      }
   }